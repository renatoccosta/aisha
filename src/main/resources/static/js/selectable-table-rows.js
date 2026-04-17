(function () {
    const interactiveSelector = "a, button, input, select, textarea, label, summary, [role='button'], [data-row-selection-ignore]";
    const rowSelector = ".responsive-list tbody tr";
    const checkboxSelector = "td.cell-select input[type='checkbox'][name='ids']";
    const selectAllToggleSelector = ".table-select-all-toggle";
    const selectedClassName = "is-selected";
    const selectableClassName = "is-selectable";
    let selectionAnchor = null;
    let preservedScrollPosition = null;
    let shouldRestorePreservedScroll = false;

    function findRowCheckbox(row) {
        return row.querySelector(checkboxSelector);
    }

    function isSelectableCheckbox(checkbox) {
        return Boolean(checkbox && !checkbox.disabled);
    }

    function getSelectionScope(checkbox) {
        return checkbox ? checkbox.closest("table") : null;
    }

    function getSelectableCheckboxes(scope) {
        if (!scope) {
            return [];
        }

        return Array.from(scope.querySelectorAll(checkboxSelector))
            .filter(isSelectableCheckbox);
    }

    function findSelectAllToggle(scope) {
        return scope ? scope.querySelector(selectAllToggleSelector) : null;
    }

    function findSelectedCountDisplay(scope) {
        if (!scope) {
            return null;
        }

        const form = scope.closest("form");
        return form ? form.querySelector("[data-selected-count-display]") : null;
    }

    function getSelectionState(scope) {
        const checkboxes = getSelectableCheckboxes(scope);
        const selectedCount = checkboxes.filter(function (checkbox) {
            return checkbox.checked;
        }).length;

        if (selectedCount === 0) {
            return "none";
        }

        if (selectedCount === checkboxes.length) {
            return "all";
        }

        return "some";
    }

    function getSelectedCount(scope) {
        return getSelectableCheckboxes(scope).filter(function (checkbox) {
            return checkbox.checked;
        }).length;
    }

    function updateSelectedCountDisplay(scope) {
        const display = findSelectedCountDisplay(scope);
        if (!display) {
            return;
        }

        const selectedCount = getSelectedCount(scope);
        const label = selectedCount === 1
            ? display.dataset.labelSingular
            : display.dataset.labelPlural;

        display.textContent = label.replace("{count}", String(selectedCount));
    }

    function updateSelectAllToggle(scope) {
        const toggle = findSelectAllToggle(scope);
        if (!toggle) {
            return;
        }

        const checkboxes = getSelectableCheckboxes(scope);
        const hasSelectableRows = checkboxes.length > 0;
        const state = hasSelectableRows ? getSelectionState(scope) : "none";

        toggle.disabled = !hasSelectableRows;
        toggle.dataset.state = state;
        toggle.setAttribute("aria-pressed", String(state === "all"));
        toggle.setAttribute("aria-label", toggle.dataset["label" + state.charAt(0).toUpperCase() + state.slice(1)]);
        toggle.title = toggle.dataset["label" + state.charAt(0).toUpperCase() + state.slice(1)];

        ["none", "some", "all"].forEach(function (iconState) {
            const icon = toggle.querySelector("[data-state-icon='" + iconState + "']");
            if (icon) {
                icon.hidden = iconState !== state;
            }
        });
    }

    function syncTableState(scope) {
        syncAllRows(scope);
        updateSelectAllToggle(scope);
        updateSelectedCountDisplay(scope);
    }

    function getActiveAnchor() {
        if (!isSelectableCheckbox(selectionAnchor) || !document.contains(selectionAnchor)) {
            selectionAnchor = null;
        }

        return selectionAnchor;
    }

    function dispatchSelectionChange(checkbox) {
        checkbox.dispatchEvent(new Event("change", { bubbles: true }));
    }

    function setCheckboxState(checkbox, checked) {
        if (!isSelectableCheckbox(checkbox) || checkbox.checked === checked) {
            return;
        }

        checkbox.checked = checked;
        dispatchSelectionChange(checkbox);
    }

    function selectCheckboxRange(anchorCheckbox, currentCheckbox, checked) {
        const scope = getSelectionScope(currentCheckbox);
        if (!scope || scope !== getSelectionScope(anchorCheckbox)) {
            setCheckboxState(currentCheckbox, checked);
            return;
        }

        const checkboxes = getSelectableCheckboxes(scope);
        const startIndex = checkboxes.indexOf(anchorCheckbox);
        const endIndex = checkboxes.indexOf(currentCheckbox);

        if (startIndex === -1 || endIndex === -1) {
            setCheckboxState(currentCheckbox, checked);
            return;
        }

        const [from, to] = startIndex < endIndex
            ? [startIndex, endIndex]
            : [endIndex, startIndex];

        for (let index = from; index <= to; index += 1) {
            setCheckboxState(checkboxes[index], checked);
        }
    }

    function setAllCheckboxes(scope, checked) {
        const checkboxes = getSelectableCheckboxes(scope);
        checkboxes.forEach(function (checkbox) {
            setCheckboxState(checkbox, checked);
        });

        selectionAnchor = checked && checkboxes.length > 0 ? checkboxes[0] : null;
    }

    function syncRowState(row) {
        const checkbox = findRowCheckbox(row);
        const isSelectable = isSelectableCheckbox(checkbox);

        row.classList.toggle(selectableClassName, isSelectable);
        row.classList.toggle(selectedClassName, Boolean(isSelectable && checkbox.checked));
    }

    function syncAllRows(root) {
        const rows = (root || document).querySelectorAll(rowSelector);
        rows.forEach(syncRowState);
    }

    function syncAllTables(root) {
        const scope = root || document;
        const tables = scope.matches && scope.matches("table.responsive-list")
            ? [scope]
            : Array.from(scope.querySelectorAll("table.responsive-list"));

        tables.forEach(syncTableState);
    }

    function scheduleToastDismiss(root) {
        const scope = root || document;
        const toasts = scope.matches && scope.matches(".toast-notification[data-auto-dismiss]")
            ? [scope]
            : Array.from(scope.querySelectorAll(".toast-notification[data-auto-dismiss]"));

        toasts.forEach(function (toast) {
            if (toast.dataset.dismissScheduled === "true") {
                return;
            }

            toast.dataset.dismissScheduled = "true";
            window.setTimeout(function () {
                toast.classList.add("is-hiding");
                window.setTimeout(function () {
                    toast.remove();
                }, 220);
            }, 4000);
        });
    }

    function preserveScrollPosition() {
        preservedScrollPosition = {
            x: window.scrollX,
            y: window.scrollY
        };
        shouldRestorePreservedScroll = true;
    }

    function restorePreservedScrollPosition() {
        if (!preservedScrollPosition || !shouldRestorePreservedScroll) {
            return;
        }

        const scrollPosition = preservedScrollPosition;
        const restore = function () {
            window.scrollTo(scrollPosition.x, scrollPosition.y);
        };

        window.requestAnimationFrame(function () {
            restore();
            window.requestAnimationFrame(restore);
            window.setTimeout(restore, 0);
        });
    }

    function clearPreservedScrollPosition() {
        preservedScrollPosition = null;
        shouldRestorePreservedScroll = false;
    }

    function shouldPreserveScrollForEvent(event) {
        const trigger = event.detail && event.detail.elt;
        return Boolean(trigger && trigger.closest("[data-preserve-scroll='true']"));
    }

    document.addEventListener("click", function (event) {
        const selectAllToggle = event.target.closest(selectAllToggleSelector);
        if (selectAllToggle) {
            const table = selectAllToggle.closest("table");
            if (!table) {
                return;
            }

            const currentState = getSelectionState(table);
            const shouldSelectAll = currentState !== "all";
            setAllCheckboxes(table, shouldSelectAll);
            syncTableState(table);
            return;
        }

        const clickedCheckbox = event.target.closest(checkboxSelector);
        if (clickedCheckbox) {
            if (!isSelectableCheckbox(clickedCheckbox)) {
                return;
            }

            const anchor = getActiveAnchor();
            if (event.shiftKey && anchor && anchor !== clickedCheckbox) {
                selectCheckboxRange(anchor, clickedCheckbox, clickedCheckbox.checked);
            }

            selectionAnchor = clickedCheckbox;
            return;
        }

        if (event.target.closest(interactiveSelector)) {
            return;
        }

        const row = event.target.closest(rowSelector);
        if (!row) {
            return;
        }

        const checkbox = findRowCheckbox(row);
        if (!checkbox || checkbox.disabled) {
            return;
        }

        const nextChecked = !checkbox.checked;
        const anchor = getActiveAnchor();

        if (event.shiftKey && anchor && anchor !== checkbox) {
            selectCheckboxRange(anchor, checkbox, nextChecked);
        } else {
            setCheckboxState(checkbox, nextChecked);
        }

        selectionAnchor = checkbox;
    });

    document.addEventListener("change", function (event) {
        const checkbox = event.target.closest(checkboxSelector);
        if (!checkbox) {
            return;
        }

        const row = checkbox.closest(rowSelector);
        if (row) {
            syncRowState(row);
        }

        syncTableState(getSelectionScope(checkbox));
    });

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            syncAllTables(document);
            scheduleToastDismiss(document);
        });
    } else {
        syncAllTables(document);
        scheduleToastDismiss(document);
    }

    document.addEventListener("htmx:beforeRequest", function (event) {
        if (!shouldPreserveScrollForEvent(event)) {
            return;
        }

        preserveScrollPosition();

        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur();
        }
    });

    document.addEventListener("htmx:afterSwap", function (event) {
        syncAllTables(event.target);
        scheduleToastDismiss(event.target);
        restorePreservedScrollPosition();
    });

    document.addEventListener("htmx:afterSettle", function () {
        restorePreservedScrollPosition();
        window.setTimeout(clearPreservedScrollPosition, 0);
    });
}());
