function galleryInit() {
    if (!("galleries" in window)) {
        return;
    }

    window.gallery = lightGallery(document.body, {
        dynamic: true,
        dynamicEl: [],
        plugins: [lgZoom, lgPager, lgFullscreen]
    });
    document.body.addEventListener("lgBeforeSlide", galleryBeforeSlide);
    document.body.addEventListener("lgAfterClose", galleryAfterClose);
    $(".entry-image").click(galleryClick);

    if (window.galleryEntryId != null && window.galleryMediaId != null) {
        galleryOpen(window.galleryEntryId, window.galleryMediaId);
    }
}

function galleryBeforeSlide(event) {
    const slide = window.gallerySlides[event.detail.index];
    window.galleryMediaId = slide["id"];
    window.history.replaceState(null, "", "/post/" + window.galleryEntryId + "?media=" + window.galleryMediaId);
}

function galleryAfterClose(event) {
    window.history.replaceState(null, "", window.canonicalUrl);
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

function galleryOpen(entryId, mediaId) {
    window.galleryEntryId = entryId;
    const list = window.galleries[entryId];
    window.gallerySlides = list;
    if (list != null) {
        window.gallery.refresh(list);
        window.gallery.openGallery(galleryGetSlide(list, mediaId));
    }
}

function galleryClick(event) {
    const entryId = $(event.currentTarget).parents(".entry").attr("data-id");
    if (entryId != null) {
        const id = event.currentTarget.getAttribute("data-id");
        galleryOpen(entryId, id);
    }
    event.preventDefault();
}

$(galleryInit);
