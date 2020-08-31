class MoeraSpoilerElement extends HTMLElement {

    constructor() {
        super();
        this._button = document.createElement("button");
        this._renderButtonText(this.title);
        const shadowRoot = this.attachShadow({
            mode: 'closed'
        });
        const style = document.createElement("style");
        style.innerText = `
			button {
				color: white;
				background-color: black;
				font-weight: bold;
				border: none;
			}
		`
        shadowRoot.appendChild(this._button);
        shadowRoot.appendChild(style);
        this._button.addEventListener("click", () => {
            shadowRoot.innerHTML = "<slot/>"
            this._button = null;
        })
    }

    static get observedAttributes() {
        return ["title"];
    }

    attributeChangedCallback(name, _oldValue, _newValue) {
        if (name === "title") {
            this._renderButtonText();
        }
    }

    get title() {
        const title = this.getAttribute("title");
        return typeof title === "string" && title.length ? title : "spoiler!"
    }

    set title(title) {
        if (typeof title !== "string") {
            throw new Error("title should be string");
        }
        this.setAttribute("title", title);
    }

    _renderButtonText() {
        if (this._button) {
            this._button.innerText = this.title;
        }
    }

}

customElements.define("mr-spoiler", MoeraSpoilerElement);

