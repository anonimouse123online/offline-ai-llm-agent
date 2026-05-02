#ifndef LLAMA_H
#define LLAMA_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define LLAMA_API __attribute__((visibility("default")))
#define LLAMA_DEFAULT_SEED 0xFFFFFFFF

struct llama_model;
struct llama_context;
struct llama_sampler;
struct llama_vocab;

typedef int32_t llama_token;
typedef int32_t llama_pos;
typedef int32_t llama_seq_id;

typedef struct llama_token_data {
    llama_token id;
    float logit;
    float p;
} llama_token_data;

typedef struct llama_token_data_array {
    llama_token_data * data;
    size_t size;
    int64_t selected;
    bool sorted;
} llama_token_data_array;

typedef struct llama_batch {
    int32_t n_tokens;
    llama_token  * token;
    float        * embd;
    llama_pos    * pos;
    int32_t      * n_seq_id;
    llama_seq_id ** seq_id;
    int8_t       * logits;
} llama_batch;

typedef struct llama_model_params {
    void * devices;
    void * tensor_buft_overrides;
    int32_t n_gpu_layers;
    int32_t split_mode;
    int32_t main_gpu;
    const float * tensor_split;
    void * progress_callback;
    void * progress_callback_user_data;
    void * kv_overrides;
    bool vocab_only;
    bool use_mmap;
    bool use_direct_io;
    bool use_mlock;
    bool check_tensors;
    bool use_extra_bufts;
    bool no_host;
    bool no_alloc;
} llama_model_params;

typedef struct llama_context_params {
    uint32_t n_ctx;
    uint32_t n_batch;
    uint32_t n_ubatch;
    uint32_t n_seq_max;
    int32_t  n_threads;
    int32_t  n_threads_batch;
    int32_t  rope_scaling_type;
    int32_t  pooling_type;
    int32_t  attention_type;
    int32_t  flash_attn_type;
    float    rope_freq_base;
    float    rope_freq_scale;
    float    yarn_ext_factor;
    float    yarn_attn_factor;
    float    yarn_beta_fast;
    float    yarn_beta_slow;
    uint32_t yarn_orig_ctx;
    float    defrag_thold;
    void *   cb_eval;
    void *   cb_eval_user_data;
    int32_t  type_k;
    int32_t  type_v;
    void *   abort_callback;
    void *   abort_callback_data;
    bool embeddings;
    bool offload_kqv;
    bool no_perf;
    bool op_offload;
    bool swa_full;
    bool kv_unified;
    void * samplers;
    size_t n_samplers;
} llama_context_params;

typedef struct llama_sampler_chain_params {
    bool no_perf;
} llama_sampler_chain_params;

LLAMA_API struct llama_model_params         llama_model_default_params(void);
LLAMA_API struct llama_context_params       llama_context_default_params(void);
LLAMA_API struct llama_sampler_chain_params llama_sampler_chain_default_params(void);

LLAMA_API void llama_backend_init(void);
LLAMA_API void llama_backend_free(void);

LLAMA_API struct llama_model   * llama_model_load_from_file(const char * path_model, struct llama_model_params params);
LLAMA_API void                   llama_model_free(struct llama_model * model);
LLAMA_API struct llama_context * llama_init_from_model(struct llama_model * model, struct llama_context_params params);
LLAMA_API void                   llama_free(struct llama_context * ctx);

LLAMA_API const struct llama_vocab * llama_model_get_vocab(const struct llama_model * model);

LLAMA_API int32_t llama_tokenize(
        const struct llama_vocab * vocab,
        const char * text, int32_t text_len,
        llama_token * tokens, int32_t n_tokens_max,
        bool add_special, bool parse_special);

LLAMA_API int32_t llama_token_to_piece(
        const struct llama_vocab * vocab,
        llama_token token, char * buf, int32_t length,
        int32_t lstrip, bool special);

LLAMA_API bool llama_vocab_is_eog(const struct llama_vocab * vocab, llama_token token);

LLAMA_API struct llama_batch llama_batch_get_one(llama_token * tokens, int32_t n_tokens);
LLAMA_API int32_t            llama_decode(struct llama_context * ctx, struct llama_batch batch);

LLAMA_API struct llama_sampler * llama_sampler_chain_init(struct llama_sampler_chain_params params);
LLAMA_API void                   llama_sampler_chain_add(struct llama_sampler * chain, struct llama_sampler * smpl);
LLAMA_API llama_token            llama_sampler_sample(struct llama_sampler * smpl, struct llama_context * ctx, int32_t idx);
LLAMA_API void                   llama_sampler_free(struct llama_sampler * smpl);

LLAMA_API struct llama_sampler * llama_sampler_init_greedy(void);
LLAMA_API struct llama_sampler * llama_sampler_init_top_k(int32_t k);
LLAMA_API struct llama_sampler * llama_sampler_init_top_p(float p, size_t min_keep);
LLAMA_API struct llama_sampler * llama_sampler_init_temp(float t);

LLAMA_API void llama_memory_clear(void * mem, bool data);
LLAMA_API void * llama_get_memory(const struct llama_context * ctx);

#ifdef __cplusplus
}
#endif

#endif