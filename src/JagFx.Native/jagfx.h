#ifndef JAGFX_H
#define JAGFX_H

#include <stddef.h>
#include <stdint.h>

#ifdef _WIN32
#define JAGFX_EXPORT __declspec(dllimport)
#else
#define JAGFX_EXPORT
#endif

#ifdef __cplusplus
extern "C" {
#endif

typedef enum jagfx_status {
    JAGFX_STATUS_SUCCESS = 0,
    JAGFX_STATUS_BUFFER_TOO_SMALL = 1,
    JAGFX_STATUS_INVALID_ARGUMENT = -1,
    JAGFX_STATUS_DECODE_ERROR = -2,
    JAGFX_STATUS_RENDER_ERROR = -3
} jagfx_status;

JAGFX_EXPORT int jagfx_render_pcm16_le(
    const uint8_t* synth_data,
    size_t synth_length,
    int32_t loop_count,
    int32_t voice_filter,
    uint8_t* output_buffer,
    size_t output_buffer_length,
    size_t* bytes_written);

#ifdef __cplusplus
}
#endif

#endif
