function galleryInit() {
    if (!("galleries" in window)) {
        return;
    }

    window.gallery = lightGallery(document.body, {
        dynamic: true,
        dynamicEl: [],
        plugins: [lgZoom, lgPager, lgFullscreen]
    });
    $(".entry-image").click(galleryClick);
}

function galleryGetSlide(list, id) {
    let i = 0;
    for (let slide of list) {
        if (slide["id"] === id) {
            return i;
        }
        i++;
    }
    return 0;
}

function galleryClick(event) {
    const entryId = $(event.currentTarget).parents(".entry").attr("data-id");
    if (entryId != null) {
        const list = window.galleries[entryId];
        if (list != null) {
            window.gallery.refresh(list);
            const id = event.currentTarget.getAttribute("data-id");
            window.gallery.openGallery(galleryGetSlide(list, id));
        }
    }
    event.preventDefault();
}

$(galleryInit);
