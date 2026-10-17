#!/usr/bin/env python3
"""Extract .synth files from RuneLite cache."""

import argparse
import gzip
import bz2
import struct
from pathlib import Path
from typing import Optional, Tuple

SECTOR_SIZE = 520
HEADER_SIZE = 8
DATA_SIZE = SECTOR_SIZE - HEADER_SIZE

def read_medium(b: bytes) -> int:
    return (b[0] << 16) | (b[1] << 8) | b[2]

def read_index_entry(idx_path: Path, group_id: int) -> Optional[Tuple[int, int]]:
    with open(idx_path, "rb") as f:
        f.seek(group_id * 6)
        entry = f.read(6)
        if len(entry) < 6:
            return None
        file_size = read_medium(entry[0:3])
        first_sector = read_medium(entry[3:6])
        if file_size == 0:
            return None
        return file_size, first_sector

def read_group_from_dat2(
    dat2_path: Path,
    archive_id: int,
    group_id: int,
    file_size: int,
    first_sector: int
) -> bytes:
    buf = bytearray()
    remaining = file_size
    sector_id = first_sector
    chunk = 0

    with open(dat2_path, "rb") as f:
        while remaining > 0 and sector_id != 0:
            f.seek(sector_id * SECTOR_SIZE)
            raw = f.read(SECTOR_SIZE)
            to_take = min(remaining, DATA_SIZE)
            if len(raw) < HEADER_SIZE + to_take:
                raise IOError("Short read on dat2")

            next_sector = read_medium(raw[4:7])
            data = raw[HEADER_SIZE:HEADER_SIZE + DATA_SIZE]
            buf.extend(data[:to_take])
            remaining -= to_take
            sector_id = next_sector
            chunk += 1

    return bytes(buf)

def parse_js5_container(buf: bytes) -> Tuple[int, int, bytes, bytes]:
    compression_type = buf[0]
    comp_len = struct.unpack(">I", buf[1:5])[0]
    offset = 5

    if compression_type == 0:
        uncompressed_len = comp_len
    else:
        uncompressed_len = struct.unpack(">I", buf[offset:offset+4])[0]
        offset += 4

    payload = buf[offset:offset+comp_len]
    remainder = buf[offset+comp_len:]
    return compression_type, uncompressed_len, payload, remainder

def decompress_js5(
    compression_type: int,
    payload: bytes,
    expected_len: Optional[int] = None
) -> bytes:
    if compression_type == 0:
        data = payload
    elif compression_type == 1:
        data = bz2.decompress(payload)
    elif compression_type == 2:
        data = gzip.decompress(payload)
    else:
        raise ValueError(f"Unknown compression type {compression_type}")

    if expected_len is not None and len(data) != expected_len:
        print(f"Warning: Length mismatch - got {len(data)}, expected {expected_len}")

    return data

def extract_group(
    cache_dir: Path,
    archive_id: int,
    group_id: int,
    raw: bool = False
) -> Optional[bytes]:
    dat2_path = cache_dir / "main_file_cache.dat2"
    idx_path = cache_dir / f"main_file_cache.idx{archive_id}"

    if not idx_path.exists():
        return None

    entry = read_index_entry(idx_path, group_id)
    if entry is None:
        return None

    file_size, first_sector = entry
    container = read_group_from_dat2(dat2_path, archive_id, group_id, file_size, first_sector)

    if raw:
        return container

    compression_type, uncompressed_len, payload, _ = parse_js5_container(container)
    return decompress_js5(compression_type, payload, uncompressed_len)

REFERENCE_TABLE_ARCHIVE = 255
FLAG_NAMED = 0x1
FLAG_DIGESTS = 0x2
FLAG_LENGTHS = 0x4
FLAG_UNCOMPRESSED_CHECKSUMS = 0x8

# A sound effect group holds the synth as file 0 and, when the sound uses a
# recorded sample, the Jagex-packed Vorbis sample as file 1. The client plays
# only the last file of the group.
FILE_EXTENSIONS = {(4, 0): ".synth", (4, 1): ".jagvorbis"}

class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def u8(self) -> int:
        self.pos += 1
        return self.data[self.pos - 1]

    def u16(self) -> int:
        self.pos += 2
        return struct.unpack(">H", self.data[self.pos - 2:self.pos])[0]

    def s32(self) -> int:
        self.pos += 4
        return struct.unpack(">i", self.data[self.pos - 4:self.pos])[0]

    def big_smart(self) -> int:
        if self.data[self.pos] < 0x80:
            return self.u16()
        return self.s32() & 0x7FFFFFFF

def read_file_ids(cache_dir: Path, archive_id: int) -> dict[int, list[int]]:
    """Read the file IDs of every group from the archive's JS5 reference table."""
    data = extract_group(cache_dir, REFERENCE_TABLE_ARCHIVE, archive_id)
    if data is None:
        raise IOError(f"Reference table for idx{archive_id} not found")

    r = Reader(data)
    protocol = r.u8()
    if protocol < 5 or protocol > 7:
        raise ValueError(f"Unsupported reference table protocol {protocol}")
    if protocol >= 6:
        r.s32()
    flags = r.u8()
    count = r.big_smart if protocol >= 7 else r.u16

    group_ids = []
    last = 0
    for _ in range(count()):
        last += count()
        group_ids.append(last)

    n = len(group_ids)
    if flags & FLAG_NAMED:
        r.pos += 4 * n
    r.pos += 4 * n
    if flags & FLAG_UNCOMPRESSED_CHECKSUMS:
        r.pos += 4 * n
    if flags & FLAG_DIGESTS:
        r.pos += 64 * n
    if flags & FLAG_LENGTHS:
        r.pos += 8 * n
    r.pos += 4 * n

    file_counts = [count() for _ in group_ids]
    file_ids = {}
    for group_id, file_count in zip(group_ids, file_counts):
        ids = []
        last = 0
        for _ in range(file_count):
            last += count()
            ids.append(last)
        file_ids[group_id] = ids
    return file_ids

def split_group(data: bytes, file_count: int) -> list[bytes]:
    """Split a decompressed JS5 group into its files."""
    if file_count == 1:
        return [data]

    chunks = data[-1]
    sizes_pos = len(data) - 1 - chunks * file_count * 4
    r = Reader(data)
    r.pos = sizes_pos
    files = [bytearray() for _ in range(file_count)]
    pos = 0
    for _ in range(chunks):
        chunk_size = 0
        for file in files:
            chunk_size += r.s32()
            file.extend(data[pos:pos + chunk_size])
            pos += chunk_size
    if pos != sizes_pos:
        raise ValueError(f"Group files end at {pos}, size table starts at {sizes_pos}")
    return [bytes(file) for file in files]

def get_ids_to_extract(
    idx_path: Path,
    ids: Optional[list[int]] = None,
    start_id: Optional[int] = None,
    end_id: Optional[int] = None,
    extract_all: bool = False
) -> list[int]:
    """Determine which IDs to extract based on provided arguments."""
    if ids:
        return sorted(set(ids))

    if extract_all or (start_id is None and end_id is None):
        idx_size = idx_path.stat().st_size
        max_id = (idx_size // 6) - 1
        return list(range(0, max_id + 1))

    start = start_id if start_id is not None else 0
    end = end_id if end_id is not None else start
    return list(range(start, end + 1))

def dump_index(
    cache_dir: Path,
    archive_id: int,
    out_dir: Path,
    ids: Optional[list[int]] = None,
    start_id: Optional[int] = None,
    end_id: Optional[int] = None,
    extract_all: bool = False,
    raw: bool = False
) -> int:
    idx_path = cache_dir / f"main_file_cache.idx{archive_id}"

    if not idx_path.exists():
        print(f"Index file not found: {idx_path}")
        return 0

    index_out_dir = out_dir / f"idx{archive_id}"
    index_out_dir.mkdir(parents=True, exist_ok=True)

    ids_to_extract = get_ids_to_extract(idx_path, ids, start_id, end_id, extract_all)

    if len(ids_to_extract) == 0:
        print(f"No groups to extract from idx{archive_id}")
        return 0

    if len(ids_to_extract) == 1:
        print(f"Dumping idx{archive_id} (group {ids_to_extract[0]})...")
    else:
        print(f"Dumping idx{archive_id} ({len(ids_to_extract)} groups)...")

    file_ids = {} if raw else read_file_ids(cache_dir, archive_id)

    extracted = 0
    for group_id in ids_to_extract:
        data = extract_group(cache_dir, archive_id, group_id, raw=raw)

        if data is None:
            continue

        if raw:
            (index_out_dir / f"{group_id}.bin").write_bytes(data)
        else:
            ids = file_ids[group_id]
            for file_id, file_data in zip(ids, split_group(data, len(ids))):
                ext = FILE_EXTENSIONS.get((archive_id, file_id))
                if ext is None:
                    ext = ".synth" if file_id == 0 else f"-{file_id}.bin"
                (index_out_dir / f"{group_id}{ext}").write_bytes(file_data)
        extracted += 1

    print(f"Extracted {extracted} groups from idx{archive_id}")
    return extracted

def main():
    parser = argparse.ArgumentParser(description="Extract .synth files from RuneLite cache")
    parser.add_argument("--cache", type=Path, required=True, help="Path to RuneLite LIVE cache directory")
    parser.add_argument("--out", type=Path, default=Path("./synths"), help="Output directory")
    parser.add_argument("--indices", type=int, nargs="+", default=[4], choices=[4, 14, 15], help="Indices to dump")

    id_group = parser.add_mutually_exclusive_group()
    id_group.add_argument("--ids", type=int, nargs="+", help="Specific group IDs to extract (e.g., 5876 5844 5822)")
    id_group.add_argument("--start-id", type=int, default=None, help="Starting group ID for range")
    id_group.add_argument("--all", action="store_true", help="Extract all groups")

    parser.add_argument("--end-id", type=int, default=None, help="Ending group ID for range (requires --start-id)")
    parser.add_argument("--raw", action="store_true", help="Write raw JS5 containers")

    args = parser.parse_args()

    if args.end_id is not None and args.start_id is None:
        parser.error("--end-id requires --start-id")

    if not args.cache.exists():
        print(f"Error: Cache directory does not exist: {args.cache}")
        return 1

    dat2_path = args.cache / "main_file_cache.dat2"
    if not dat2_path.exists():
        print(f"Error: Data file not found: {dat2_path}")
        return 1

    print(f"Cache: {args.cache}")
    print(f"Output: {args.out}")
    print(f"Indices: {args.indices}")

    if args.ids:
        print(f"Mode: Specific IDs: {args.ids}")
    elif args.all:
        print(f"Mode: All files")
    elif args.start_id is not None:
        end_str = args.end_id if args.end_id is not None else args.start_id
        print(f"Mode: Range {args.start_id}-{end_str}")
    else:
        print(f"Mode: Single ID 0 (default)")

    args.out.mkdir(parents=True, exist_ok=True)

    total_extracted = 0
    for archive_id in args.indices:
        extracted = dump_index(
            args.cache,
            archive_id,
            args.out,
            ids=args.ids,
            start_id=args.start_id,
            end_id=args.end_id,
            extract_all=args.all,
            raw=args.raw
        )
        total_extracted += extracted

    print(f"Total: {total_extracted} groups extracted")
    return 0

if __name__ == "__main__":
    exit(main())
