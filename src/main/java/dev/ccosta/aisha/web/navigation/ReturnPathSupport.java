package dev.ccosta.aisha.web.navigation;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds and validates local return paths used to bring the user back to the originating listing.
 */
public final class ReturnPathSupport {

    private ReturnPathSupport() {}

    /**
     * Builds a local return path from the provided listing state, omitting blank values.
     *
     * @param basePath local listing path
     * @param parameters alternating query parameter names and values
     * @return local path with query string when applicable
     */
    public static String buildReturnPath(String basePath, Object... parameters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        for (int index = 0; index < parameters.length; index += 2) {
            String name = String.valueOf(parameters[index]);
            Object value = parameters[index + 1];
            if (value == null) {
                continue;
            }
            if (value instanceof String stringValue && !StringUtils.hasText(stringValue)) {
                continue;
            }
            builder.queryParam(name, value);
        }
        return builder.build().encode().toUriString();
    }

    /**
     * Resolves a user-provided return path into a safe redirect target, falling back to the listing base path when invalid.
     *
     * @param returnTo desired local return path
     * @param fallbackPath local fallback listing path
     * @return redirect view name
     */
    public static String resolveRedirect(String returnTo, String fallbackPath) {
        return "redirect:" + resolveReturnPath(returnTo, fallbackPath);
    }

    /**
     * Resolves a user-provided return path into a safe local path.
     *
     * @param returnTo desired local return path
     * @param fallbackPath local fallback listing path
     * @return safe local path
     */
    public static String resolveReturnPath(String returnTo, String fallbackPath) {
        if (!StringUtils.hasText(returnTo)) {
            return fallbackPath;
        }

        String normalized = returnTo.trim();
        if (!normalized.startsWith("/") || normalized.startsWith("//")) {
            return fallbackPath;
        }
        if (normalized.contains("\r") || normalized.contains("\n")) {
            return fallbackPath;
        }
        return normalized;
    }
}
