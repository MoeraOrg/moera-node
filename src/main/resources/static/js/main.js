function init() {
    $("#register-submit").click(function (event) {
        event.preventDefault();
        let data = JSON.stringify({
            "name": $("#register-name").val()
        });
        $.ajax({
            method: "POST",
            url: "/moera-node/registered-name",
            contentType: "application/json",
            data: data
        });
    });
}

$(init);