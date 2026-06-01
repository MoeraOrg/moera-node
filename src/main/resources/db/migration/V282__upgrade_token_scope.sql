UPDATE tokens SET auth_scope = x'7fffffff'::bigint WHERE auth_scope = x'3fffffff'::bigint;
