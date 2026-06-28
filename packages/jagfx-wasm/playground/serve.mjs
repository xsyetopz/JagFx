#!/usr/bin/env bun
import { existsSync } from "node:fs";
import { extname, join, normalize, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

const packageDir = resolve(fileURLToPath(new URL("..", import.meta.url)));
const repoRoot = resolve(packageDir, "../..");
const playgroundDir = join(packageDir, "playground");
const port = Number.parseInt(process.env.PORT ?? "8787", 10);
const host = process.env.HOST ?? "127.0.0.1";
const wwwroot = resolveWwwroot();

if (process.env.JAGFX_PLAYGROUND_SMOKE === "1") {
	console.log(`JagFx WASM playground smoke OK`);
	console.log(`Serving JagFx.Wasm wwwroot: ${wwwroot}`);
	process.exit(0);
}

Bun.serve({
	port,
	hostname: host,
	async fetch(request) {
		try {
			const url = new URL(request.url);
			const pathname = decodeURIComponent(url.pathname);

			if (pathname.startsWith("/_framework/")) {
				return await responseFromFile(wwwroot, pathname.slice(1));
			}

			if (pathname.startsWith("/dist/")) {
				return await responseFromFile(packageDir, pathname.slice(1));
			}

			if (pathname.startsWith("/references/")) {
				return await responseFromFile(repoRoot, pathname.slice(1));
			}

			const playgroundPath =
				pathname === "/" ? "index.html" : pathname.slice(1);
			return await responseFromFile(playgroundDir, playgroundPath);
		} catch (error) {
			const message = error instanceof Error ? error.message : String(error);
			const statusCode = message.includes("not found") ? 404 : 500;
			return new Response(`${statusCode} ${message}\n`, {
				status: statusCode,
				headers: { "content-type": "text/plain; charset=utf-8" },
			});
		}
	},
});

console.log(`JagFx WASM playground: http://${host}:${port}/`);
console.log(`Serving JagFx.Wasm wwwroot: ${wwwroot}`);

function resolveWwwroot() {
	const candidates = [
		process.env.JAGFX_WASM_WWWROOT,
		join(repoRoot, "src/JagFx.Wasm/bin/Release/net8.0-browser/publish/wwwroot"),
		join(repoRoot, "src/JagFx.Wasm/bin/Release/net8.0-browser/wwwroot"),
		join(
			repoRoot,
			"src/JagFx.Wasm/bin/Release/net8.0/browser-wasm/publish/wwwroot",
		),
		join(repoRoot, "src/JagFx.Wasm/bin/Release/net8.0/publish/wwwroot"),
	].filter(Boolean);

	const found = candidates.find((candidate) =>
		existsSync(join(candidate, "_framework/dotnet.js")),
	);
	if (found) {
		return resolve(found);
	}

	throw new Error(
		[
			"JagFx.Wasm publish output not found.",
			"Run `bun --cwd packages/jagfx-wasm run build:wasm` first, or set JAGFX_WASM_WWWROOT=/path/to/wwwroot.",
			"Checked:",
			...candidates.map((candidate) => `  - ${candidate}`),
		].join("\n"),
	);
}

async function responseFromFile(root, relativePath) {
	const filePath = safeJoin(root, relativePath);
	const file = Bun.file(filePath);
	if (!(await file.exists())) {
		throw new Error(`not found: /${relativePath}`);
	}

	return new Response(file, {
		headers: {
			"content-type": contentType(filePath),
			"cross-origin-embedder-policy": "require-corp",
			"cross-origin-opener-policy": "same-origin",
		},
	});
}

function safeJoin(root, relativePath) {
	const normalized = normalize(relativePath).replace(/^([/\\])+/, "");
	const filePath = resolve(root, normalized);
	const rootWithSep = root.endsWith(sep) ? root : `${root}${sep}`;
	if (filePath !== root && !filePath.startsWith(rootWithSep)) {
		throw new Error("path traversal rejected");
	}

	return filePath;
}

function contentType(filePath) {
	switch (extname(filePath)) {
		case ".html":
			return "text/html; charset=utf-8";
		case ".js":
		case ".mjs":
			return "text/javascript; charset=utf-8";
		case ".json":
			return "application/json; charset=utf-8";
		case ".wasm":
			return "application/wasm";
		case ".dat":
		case ".synth":
			return "application/octet-stream";
		case ".css":
			return "text/css; charset=utf-8";
		default:
			return "application/octet-stream";
	}
}
