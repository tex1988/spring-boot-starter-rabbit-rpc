package io.github.tex1988.boot.rpc.rabbit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @SneakyThrows
    public static Class<?> getClassByName(Object self, String name) {
        return self.getClass().getClassLoader().loadClass(name);
    }

    public static Map.Entry<Method, MethodHandle> getMethodEntry(Map<Class<?>, Map<Method, MethodHandle>> methodHandles,
                                                                 Class<?> clazz, String methodName, Object[] args) {
        Class<?>[] argTypes = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class<?>[]::new);
        Map<Method, MethodHandle> iMethodHandles = methodHandles.get(clazz);
        return iMethodHandles.entrySet().stream()
                .filter(e -> e.getKey().getName().equals(methodName))
                .filter(e -> e.getKey().getParameterTypes().length == argTypes.length)
                .filter(e -> {
                    Class<?>[] paramTypes = e.getKey().getParameterTypes();
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (!paramTypes[i].isAssignableFrom(argTypes[i])) {
                            return false;
                        }
                    }
                    return true;
                })
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Method: " + methodName + " not found"));
    }

    public static Long getTimestamp() {
        return LocalDateTime.now()
                .atZone(ZONE_ID)
                .toInstant()
                .toEpochMilli();
    }
}
