function hostChanged(event) {
    if (event != null) {
        $("#help").removeClass("d-none");
        $("#feedback").removeClass("d-block").addClass("d-none").removeAttr("id");
    }
    if ($("#feedback").length) {
        $("#host-submit").attr("disabled", true);
        $("#host").addClass("is-invalid");
        return;
    }
    const host = $("#host").val();
    const valid = host.match(/^[a-z][a-z0-9-]*$/i);
    $("#host-submit").attr("disabled", !valid);
    $("#host").removeClass("is-valid is-invalid");
    if (host !== "") {
        $("#host").addClass(valid ? "is-valid" : "is-invalid");
    }
}

function registrarInit() {
    hostChanged(null);
    $("#host").keyup(hostChanged);
    $("#host").on("input", hostChanged);
}

$(registrarInit);
