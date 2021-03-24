function shareInit() {
    $(".share-button").each(function(index, button) {
        const popup = $(this).next(".share-popup");

        if (navigator.share) {
            const popupBody = popup.children(".shareon");
            const title = popupBody.attr("data-title");
            const url = popupBody.attr("data-url");
            $(this).click(function() {
                navigator.share({ title, url });
            });
            return;
        }

        const popper = Popper.createPopper(this, popup.get(0), {"placement": "top" });

        const hide = function() {
            popup.removeClass("show");
            popper.setOptions({modifiers: [{ name: 'eventListeners', enabled: false }]});
        }
        const onDocumentClick = function(event) {
            if (!popup.get(0).contains(event.target) && !button.contains(event.target)) {
                hide();
                $("body").off("click", this);
                event.preventDefault();
            }
        }
        const show = function() {
            popup.addClass("show");
            popper.setOptions({modifiers: [{ name: 'eventListeners', enabled: true }]});
            popper.update();
        }

        $(this).click(function() {
            if (popup.hasClass("show")) {
                hide();
                $("body").off("click", onDocumentClick);
            } else {
                show();
                $("body").on("click", onDocumentClick);
            }
        });
    });
}

$(shareInit);
