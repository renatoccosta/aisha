(function () {
    function renderIcons() {
        if (window.lucide && typeof window.lucide.createIcons === "function") {
            window.lucide.createIcons();
        }
    }

    window.aishaRenderIcons = renderIcons;

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", renderIcons);
    } else {
        renderIcons();
    }

    document.addEventListener("htmx:afterSwap", renderIcons);
}());
