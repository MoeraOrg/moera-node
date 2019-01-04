function initIndex() {
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
        $("#register-name").val("");
    });
}

function initCredentials() {
    $.getJSON("/moera-node/credentials", function (data) {
        if (data.created) {
            $("#update").css("display", "block");
        } else {
            $("#create").css("display", "block");
        }
    }); // TODO .fail()
}

function init() {
    switch (window.pageName) {
        case "index":
            initIndex();
            break;
        case "credentials":
            initCredentials();
            break;
    }
}

$(init);