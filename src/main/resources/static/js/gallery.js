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

    if (window.galleryPostingId != null && window.galleryMediaId != null) {
        galleryOpen(window.galleryPostingId, window.galleryCommentId, window.galleryMediaId);
    }
}

function galleryBeforeSlide(event) {
    const slide = window.gallerySlides[event.detail.index];
    window.galleryMediaId = slide["id"];
    const url = "/post/" + window.galleryPostingId + "?"
        + (window.galleryCommentId != null ? "comment=" + window.galleryCommentId + "&" : "")
        + "media=" + window.galleryMediaId;
    window.history.replaceState(null, "", url);
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

function galleryOpen(postingId, commentId, mediaId) {
    window.galleryPostingId = postingId;
    window.galleryCommentId = commentId;
    const list = window.galleries[commentId ?? postingId];
    window.gallerySlides = list;
    if (list != null) {
        window.gallery.refresh(list);
        window.gallery.openGallery(galleryGetSlide(list, mediaId));
    }
}

function galleryClick(event) {
    const entry = $(event.currentTarget).parents(".entry");
    const postingId = entry.attr("data-post-id");
    const commentId = entry.attr("data-comment-id");
    if (postingId != null) {
        const id = event.currentTarget.getAttribute("data-id");
        galleryOpen(postingId, commentId, id);
    }
    event.preventDefault();
}

$(galleryInit);
