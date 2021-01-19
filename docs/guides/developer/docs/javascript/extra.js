/**
 * This function will be executed twice, because there are 2 documents.
 * The outer document (navigation bar on top and TOC on the left side on the screen)
 * and the inner document, the page content.
 * We execute our functions only at the inner document.
 */
$(document).ready(function () {
    // check if the current document is an iframe..
    // then we know, that we have the inner document
    if (window.location !== window.parent.location) {
        addTitleToCodeTag();
        addCopyToClipboardButton();
    } else {
        (function (i, s, o, g, r, a, m) {
            i['GoogleAnalyticsObject'] = r;
            i[r] = i[r] || function () {
                (i[r].q = i[r].q || []).push(arguments)
            }, i[r].l = 1 * new Date();
            a = s.createElement(o),
            m = s.getElementsByTagName(o)[0];
            a.async = 1;
            a.src = g;
            m.parentNode.insertBefore(a, m)
        })(window, document, 'script', 'https://www.google-analytics.com/analytics.js', 'ga');
        ga('create', 'UA-120509325-1', 'auto');
        ga('send', 'pageview');
    }
    tocPaneFix();

    // Display a hint that the development docs are outdated unless we are on develop.
    if (window.location.pathname.startsWith('/r/')) {
        let path = window.location.pathname.replace(/^\/r\/[0-9]+\.*x/, '/develop')
        let page_content = $('.wm-page-content').prepend(
            `<div style="padding: 20px; border-left: 3px solid silver;">
            This is an outdated version of the development documentation for Opencast.
            The latest version of the docs can be found <a href="${path}">here</a>.</div>`)
    }
});


/**
 * Fixes the issue that you can click on the toc elements, while the toc pane on the left side is closed
 */
function tocPaneFix() {
    $('#wm-toc-button').on('click', function (e) {
        if (!isSmallScreen()) {
            $('.wm-toc-pane').toggleClass('hidden');
        }
    })
}

/**
 * Adds a tooltip to all <code> block containing "etc/" via title tag
 */
function addTitleToCodeTag() {
    var SPAN = document.createElement("SPAN");
    SPAN.innerText = "etc";
    SPAN.classList.add("etc-span");
    SPAN.title = '"etc" represents the configuration directory of Opencast.\nThis directory is often located at "/etc/opencast".';

    var codeElementList = document.getElementsByTagName("CODE");
    for (var i = 0; i < codeElementList.length; i++) {
        var CODE = codeElementList[i];
        if (typeof CODE.innerText !== 'undefined') {
            if (CODE.innerText.startsWith('etc/')) {
                CODE.innerHTML = CODE.innerHTML.replace(/^etc/, SPAN.outerHTML);
            }
        }
    }
}

/**
 * Adds a copy-to-clipboard button to all <code> blocks (not one liners)
 */
function addCopyToClipboardButton() {
    var idPrefix = "codeId_";
    var idNumerator = 0;

    var codeElementList = document.getElementsByTagName("CODE");

    for (var i = 0; i < codeElementList.length; i++) {
        var CODE = codeElementList[i];
        if (typeof CODE.innerText !== 'undefined') {
            var PRE = CODE.parentElement;
            if (PRE.tagName !== 'PRE') continue;
            var id = idPrefix + (++idNumerator);
            var TEXTAREA = createTextArea(id, CODE.innerText);
            var BUTTON = createButton(id);
            PRE.insertBefore(BUTTON, PRE.childNodes[0] || null);
            PRE.append(TEXTAREA);
        }
    }
}

/**
 * Creates an invisible textarea. This is a workaround, because you just can
 * copy text to the clipboard from an input field or textarea. So we create an invisible one
 * and copy the content of the <code> block into it.
 *
 * @param {String} id   The id of the textarea. It serves the purpose to memorize the relation
 *                      between button and textarea.
 * @param {String} text The text from the <code> block.
 *
 * @returns {HTMLElement} The generated TextArea.
 */
function createTextArea(id, text) {
    var TEXTAREA = document.createElement("TEXTAREA");
    TEXTAREA.textContent = text;
    TEXTAREA.id = id;
    TEXTAREA.classList.add("invisible-text-area");
    return TEXTAREA;
}

/**
 * Outsourced code to generate a Button
 *
 * @param id The ID of the button.
 *
 * @returns {HTMLButtonElement} The generated Button.
 */

function createButton(id) {
    var button = document.createElement("button");
    button.addEventListener('click', function () {
            copyToClipBoard(id);
        }, false
    );
    button.classList.add("glyphicon");
    button.classList.add("glyphicon-copy");
    button.classList.add("click-and-copy-button");
    return button;
}

/**
 * Copies the content of an textarea into the clipboard.
 *
 * @param id The ID of the textarea.
 */
function copyToClipBoard(id) {
    var TEXTAREA = document.getElementById(id);
    TEXTAREA.select();
    document.execCommand("copy");
}
