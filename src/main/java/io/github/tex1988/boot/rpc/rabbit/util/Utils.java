package io.github.tex1988.boot.rpc.rabbit.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.util.ClassUtils;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.tex1988.boot.rpc.rabbit.constant.Constants.DEFAULT_ALLOWED_SERIALIZATION_PATTERNS;

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
                .map(obj -> obj == null ? null : obj.getClass())
                .toArray(Class<?>[]::new);
        Map<Method, MethodHandle> iMethodHandles = methodHandles.get(clazz);
        return iMethodHandles.entrySet().stream()
                .filter(e -> e.getKey().getName().equals(methodName))
                .filter(e -> isArgsMatch(e, argTypes))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Method: " + methodName + " not found"));
    }

    public static List<String> getAllowedClassesNames(String[] patterns) {
        if (patterns != null) {
            patterns = Stream.concat(Arrays.stream(patterns), DEFAULT_ALLOWED_SERIALIZATION_PATTERNS.stream())
                    .toArray(String[]::new);
        } else {
            patterns = DEFAULT_ALLOWED_SERIALIZATION_PATTERNS.toArray(String[]::new);
        }
        return Arrays.stream(patterns).map(Utils::scanClasses)
                .flatMap(List::stream)
                .toList();
    }

    public static Long getTimestamp() {
        return LocalDateTime.now()
                .atZone(ZONE_ID)
                .toInstant()
                .toEpochMilli();
    }

    private static boolean isArgsMatch(Map.Entry<Method, MethodHandle> e, Class<?>[] argTypes) {
        Class<?>[] paramTypes = e.getKey().getParameterTypes();
        if (paramTypes.length != argTypes.length) {
            return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (argTypes[i] == null) {
                continue;
            }
            if (!ClassUtils.isAssignable(paramTypes[i], argTypes[i])) {
                return false;
            }
        }
        return true;
    }

    private static List<String> scanClasses(String basePackage) {
        if (!basePackage.endsWith(".*")) {
            return List.of(basePackage);
        }
        basePackage = basePackage.substring(0, basePackage.length() - 2);
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(basePackage)
                .enableSystemJarsAndModules()
                .ignoreClassVisibility()
                .scan()) {
            return scanResult.getAllStandardClasses()
                    .stream()
                    .map(ClassInfo::getName)
                    .toList();
        }
    }
}
