package com.lincoco.springai.zhinao.aot;

import org.springframework.ai.aot.AiRuntimeHints;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

public class ZhinaoRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
        MemberCategory[] mcs = MemberCategory.values();
        for (TypeReference typeReference : AiRuntimeHints.findJsonAnnotatedClassesInPackage("com.lincoco.zhinao.springai")) {
            hints.reflection().registerType(typeReference, mcs);
        }
    }
}
