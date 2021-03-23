function shareInit() {
    $(".share-button").each(function() {
        const popup = $(this).next(".share-popup");
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
        })
    });
}

$(shareInit);
