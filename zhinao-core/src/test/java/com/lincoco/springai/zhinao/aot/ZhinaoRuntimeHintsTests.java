package com.lincoco.springai.zhinao.aot;

import com.lincoco.springai.zhinao.ZhinaoChatOptions;
import com.lincoco.springai.zhinao.aot.ZhinaoRuntimeHints;
import com.lincoco.springai.zhinao.api.ZhinaoApi;
import org.junit.jupiter.api.Test;
import org.springframework.ai.aot.AiRuntimeHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class ZhinaoRuntimeHintsTests {

    @Test
    void registerHints() {
        RuntimeHints runtimeHints = new RuntimeHints();

        ZhinaoRuntimeHints zhinaoRuntimeHints = new ZhinaoRuntimeHints();

        zhinaoRuntimeHints.registerHints(runtimeHints, null);

        Set<TypeReference> jsonAnnotatedClasses = AiRuntimeHints.findJsonAnnotatedClassesInPackage("com.lincoco.zhinao.springai");

        Set<TypeReference> registeredTypes = new HashSet<>();
        runtimeHints.reflection().typeHints().forEach(typeHint -> registeredTypes.add(typeHint.getType()));

        for (TypeReference jsonAnnotatedClass : jsonAnnotatedClasses) {
            assertThat(registeredTypes.contains(jsonAnnotatedClass)).isTrue();
        }

        assertThat(registeredTypes.contains(TypeReference.of(ZhinaoApi.ChatCompletion.class))).isTrue();
        assertThat(registeredTypes.contains(TypeReference.of(ZhinaoApi.ChatCompletionRequest.class))).isTrue();
        assertThat(registeredTypes.contains(TypeReference.of(ZhinaoApi.ChatCompletionChunk.class))).isTrue();
        assertThat(registeredTypes.contains(TypeReference.of(ZhinaoApi.Usage.class))).isTrue();
        assertThat(registeredTypes.contains(TypeReference.of(ZhinaoChatOptions.class))).isTrue();
    }
}
