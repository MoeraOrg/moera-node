function shareInit() {
    $(".share-button").each(function() {
        const popup = $(this).next(".share-popup");
        if (navigator.share) {
            const popupBody = popup.children(".shareon");
            const title = popupBody.attr("data-title");
            const url = popupBody.attr("data-url");
            $(this).click(function() {
                navigator.share({ title, url });
            });
        } else {
            const popper = Popper.createPopper(this, popup.get(0), {"placement": "top" });
            $(this).click(function() {
                if (popup.hasClass("show")) {
                    popup.removeClass("show");
                    popper.setOptions({modifiers: [{ name: 'eventListeners', enabled: false }]});
                } else {
                    popup.addClass("show");
                    popper.setOptions({modifiers: [{ name: 'eventListeners', enabled: true }]});
                    popper.update();
                }
            });
        }
    });
}

$(shareInit);
