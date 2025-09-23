/*
Copyright 2025 lin-coco

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
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
