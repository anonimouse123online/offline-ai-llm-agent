#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "JarvisLLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static llama_model* model = nullptr;
static llama_context* ctx = nullptr;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_jarvisapp_LlamaEngine_initModel(
        JNIEnv* env, jobject, jstring modelPath) {

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s", path);

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;

    model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Failed to load model");
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 512;
    ctx_params.n_threads = 4;
    ctx_params.n_batch = 512;

    ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        return 0;
    }

    LOGI("Model loaded successfully");
    return (jlong)model;
}

JNIEXPORT jstring JNICALL
Java_com_example_jarvisapp_LlamaEngine_runInference(
        JNIEnv* env, jobject,
        jlong modelHandle,
        jstring systemPrompt,
        jstring userInput) {

    if (!model || !ctx) {
        return env->NewStringUTF("{\"tool\":\"unknown\"}");
    }

    const char* sys = env->GetStringUTFChars(systemPrompt, nullptr);
    const char* usr = env->GetStringUTFChars(userInput, nullptr);

    std::string prompt = "<|system|>\n";
    prompt += sys;
    prompt += "\n<|user|>\n";
    prompt += usr;
    prompt += "\n<|assistant|>\n";

    env->ReleaseStringUTFChars(systemPrompt, sys);
    env->ReleaseStringUTFChars(userInput, usr);

    const llama_vocab* vocab = llama_model_get_vocab(model);
    std::vector<llama_token> tokens(512);
    int n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.size(),
                                  tokens.data(), tokens.size(), true, true);

    if (n_tokens < 0) {
        LOGE("Tokenization failed");
        return env->NewStringUTF("{\"tool\":\"unknown\"}");
    }
    tokens.resize(n_tokens);
    LOGI("Tokenized prompt: %d tokens", n_tokens);

    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t)tokens.size());
    if (llama_decode(ctx, batch) != 0) {
        LOGE("llama_decode failed on prompt");
        return env->NewStringUTF("{\"tool\":\"unknown\"}");
    }

    // Set up sampler chain once (not inside loop)
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler* sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    std::string result;
    const int max_tokens = 200;

    for (int i = 0; i < max_tokens; i++) {
        llama_token token_id = llama_sampler_sample(sampler, ctx, -1);

        if (llama_vocab_is_eog(vocab, token_id)) {
            LOGI("EOG token reached at step %d", i);
            break;
        }

        char buf[256];
        int len = llama_token_to_piece(vocab, token_id, buf, sizeof(buf), 0, true);
        if (len > 0) result.append(buf, len);

        llama_batch next = llama_batch_get_one(&token_id, 1);
        if (llama_decode(ctx, next) != 0) {
            LOGE("llama_decode failed at token %d", i);
            break;
        }
    }

    LOGI("Generated response: %s", result.c_str());

    // Free sampler and clear memory
    llama_sampler_free(sampler);
    llama_memory_clear(llama_get_memory(ctx), false);

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_jarvisapp_LlamaEngine_freeModel(
        JNIEnv*, jobject, jlong) {
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    if (model) { llama_model_free(model); model = nullptr; }
    llama_backend_free();
    LOGI("Model freed");
}

} // extern "C"