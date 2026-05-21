#include <node_api.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <windows.h>
#else
#include <dlfcn.h>
#endif

typedef int (*jagfx_render_pcm16_le_fn)(
    const uint8_t* synth_data,
    size_t synth_length,
    int32_t loop_count,
    int32_t voice_filter,
    uint8_t* output_buffer,
    size_t output_buffer_length,
    size_t* bytes_written);

static jagfx_render_pcm16_le_fn render_pcm16_le = NULL;
static void* library_handle = NULL;

static napi_value throw_error(napi_env env, const char* code, const char* message)
{
    napi_value error;
    napi_value code_value;
    napi_create_string_utf8(env, message, NAPI_AUTO_LENGTH, &error);
    napi_throw_error(env, code, message);
    napi_create_string_utf8(env, code, NAPI_AUTO_LENGTH, &code_value);
    return NULL;
}

static int load_library(napi_env env)
{
    if (render_pcm16_le != NULL)
        return 1;

    const char* path = getenv("JAGFX_NATIVE_LIB");
    if (path == NULL || path[0] == '\0')
    {
        throw_error(env, "JAGFX_NATIVE_LIB_MISSING", "Set JAGFX_NATIVE_LIB to the JagFx native library path.");
        return 0;
    }

#ifdef _WIN32
    library_handle = (void*)LoadLibraryA(path);
    if (library_handle == NULL)
    {
        throw_error(env, "JAGFX_NATIVE_LOAD_FAILED", "Failed to load JAGFX_NATIVE_LIB.");
        return 0;
    }
    render_pcm16_le = (jagfx_render_pcm16_le_fn)GetProcAddress((HMODULE)library_handle, "jagfx_render_pcm16_le");
#else
    library_handle = dlopen(path, RTLD_NOW);
    if (library_handle == NULL)
    {
        throw_error(env, "JAGFX_NATIVE_LOAD_FAILED", dlerror());
        return 0;
    }
    render_pcm16_le = (jagfx_render_pcm16_le_fn)dlsym(library_handle, "jagfx_render_pcm16_le");
#endif

    if (render_pcm16_le == NULL)
    {
        throw_error(env, "JAGFX_NATIVE_SYMBOL_MISSING", "jagfx_render_pcm16_le was not found in JAGFX_NATIVE_LIB.");
        return 0;
    }

    return 1;
}

static int32_t read_int_option(napi_env env, napi_value options, const char* name, int32_t default_value)
{
    bool has_property = false;
    napi_value property;
    napi_value key;
    napi_create_string_utf8(env, name, NAPI_AUTO_LENGTH, &key);
    napi_has_property(env, options, key, &has_property);
    if (!has_property)
        return default_value;

    napi_get_property(env, options, key, &property);
    int32_t value;
    if (napi_get_value_int32(env, property, &value) != napi_ok)
        return default_value;

    return value;
}

static napi_value render_synth_to_pcm(napi_env env, napi_callback_info info)
{
    size_t argc = 2;
    napi_value argv[2];
    napi_get_cb_info(env, info, &argc, argv, NULL, NULL);

    if (argc < 1)
        return throw_error(env, "JAGFX_INVALID_ARGUMENT", "A synth Buffer is required.");

    bool is_buffer = false;
    napi_is_buffer(env, argv[0], &is_buffer);
    if (!is_buffer)
        return throw_error(env, "JAGFX_INVALID_ARGUMENT", "The first argument must be a Buffer.");

    uint8_t* input_data;
    size_t input_length;
    napi_get_buffer_info(env, argv[0], (void**)&input_data, &input_length);

    int32_t loop_count = 1;
    int32_t voice_filter = -1;
    if (argc >= 2)
    {
        napi_valuetype type;
        napi_typeof(env, argv[1], &type);
        if (type == napi_object)
        {
            loop_count = read_int_option(env, argv[1], "loops", 1);
            voice_filter = read_int_option(env, argv[1], "voiceFilter", -1);
        }
    }

    if (!load_library(env))
        return NULL;

    size_t bytes_written = 0;
    int status = render_pcm16_le(input_data, input_length, loop_count, voice_filter, NULL, 0, &bytes_written);
    if (status != 1 || bytes_written == 0)
        return throw_error(env, "JAGFX_RENDER_FAILED", "JagFx failed to report a required output buffer size.");

    void* output_data;
    napi_value output;
    napi_create_buffer(env, bytes_written, &output_data, &output);

    status = render_pcm16_le(input_data, input_length, loop_count, voice_filter, output_data, bytes_written, &bytes_written);
    if (status != 0)
        return throw_error(env, "JAGFX_RENDER_FAILED", "JagFx failed to render PCM output.");

    return output;
}

static napi_value init(napi_env env, napi_value exports)
{
    napi_value fn;
    napi_create_function(env, "renderSynthToPcm", NAPI_AUTO_LENGTH, render_synth_to_pcm, NULL, &fn);
    napi_set_named_property(env, exports, "renderSynthToPcm", fn);
    return exports;
}

NAPI_MODULE(NODE_GYP_MODULE_NAME, init)
