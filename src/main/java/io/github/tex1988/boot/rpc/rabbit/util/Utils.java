package io.github.tex1988.boot.rpc.rabbit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    @SneakyThrows
    public static Class<?> getClassByName(Object self, String name) {
        return self.getClass().getClassLoader().loadClass(name);
    }

    public static Map.Entry<Method, MethodHandle> getMethodEntry(Map<Class<?>, Map<Method, MethodHandle>> methodHandles, Class<?> clazz, String methodName) {
        Map<Method, MethodHandle> iMethodHandles = methodHandles.get(clazz);
        return iMethodHandles.entrySet().stream()
                .filter(e -> e.getKey().getName().equals(methodName))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Method: " + methodName + " not found"));
    }
}
