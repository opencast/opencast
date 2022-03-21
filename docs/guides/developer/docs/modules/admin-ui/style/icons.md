## Icons

The icon set is carefully designed for simplicity to convey a clear
and concise user experience. Having a simple but effective icon set is crucial for ease
of use within sophisticated software environments.

Some icons are saved as pre-rendered images and can be found in the resource folder (`.../src/main/webapp/img`). Other
icons are based on [Font Awesome](http://fontawesome.io/) which gives us scalable vector icons that can be customized by
size, color, drop shadow, and anything that is provided by CSS.

For more links and license information refer to [References - Font
Awesome](/modules/admin-ui/style/references/#font-awesome).

**Note:** Font Awesome contains over 630 icons, [the cheat sheet of icons](http://fontawesome.io/cheatsheet/) lists the
icons and the corresponding CSS class.

<div class="row">
  <div class="col-4">
    <i class="fa fa-camera-retro fa-lg"></i>
    <i class="fa fa-camera-retro fa-2x"></i>
    <i class="fa fa-camera-retro fa-3x"></i>
    <i class="fa fa-camera-retro fa-4x"></i>
    <i class="fa fa-camera-retro fa-5x"></i>
  </div>
  <div class="col-3" style="padding-left: 50px;">
    <i class="fa fa-shield"></i> normal<br>
    <i class="fa fa-shield fa-rotate-90"></i> fa-rotate-90<br>
    <i class="fa fa-shield fa-rotate-180"></i> fa-rotate-180<br>
    <i class="fa fa-shield fa-rotate-270"></i> fa-rotate-270<br>
    <i class="fa fa-shield fa-flip-horizontal"></i> fa-flip-horizontal<br>
    <i class="fa fa-shield fa-flip-vertical"></i> fa-flip-vertical
  </div>
  <div class="col-4">
    <span class="fa-stack fa-lg">
      <i class="fa fa-square-o fa-stack-2x"></i>
      <i class="fa fa-twitter fa-stack-1x"></i>
    </span>
    fa-twitter on fa-square-o<br>
    <span class="fa-stack fa-lg">
      <i class="fa fa-circle fa-stack-2x"></i>
      <i class="fa fa-flag fa-stack-1x fa-inverse"></i>
    </span>
    fa-flag on fa-circle<br>
    <span class="fa-stack fa-lg">
      <i class="fa fa-square fa-stack-2x"></i>
      <i class="fa fa-terminal fa-stack-1x fa-inverse"></i>
    </span>
    fa-terminal on fa-square<br>
    <span class="fa-stack fa-lg">
      <i class="fa fa-camera fa-stack-1x"></i>
      <i class="fa fa-ban fa-stack-2x text-danger"></i>
    </span>
    fa-ban on fa-camera
  </div>
</div>
<br/>

#### Section Navigation Tab Icons

These icons are to represent different sections within the UI and
should only be represented in a “flat” graphic style. Simplicity is
key in the design of section icons. The user must easily identify
the icons. Extension of these icons must represent a similar style.

<div class="icons">
  <div>
    <pre>
      <code class="hljs css">
        <span class="hljs-comment">/* [ Inactive ] */</span>
        <span class="hljs-rule">
          <span class="hljs-attribute">color</span>
          <span class="hljs-rule">:</span>
          <span class="hljs-value">
            <span class="hljs-hexcolor"> #C6C6C6</span>
          </span>
        </span>;
      </code>
    </pre>
  </div>
  <img src="../../../../img/dashboard_2x.png" alt="Dashboard"/>
  <img src="../../../../img/user-group_2x.png" alt="Users"/>
  <img src="../../../../img/system_2x.png" alt="System"/>
  <img src="../../../../img/servers_2x.png" alt="Servers"/>
  <img src="../../../../img/events_2x.png" alt="Events"/>
  <img src="../../../../img/recordings_2x.png" alt="Recordings"/>
  <img src="../../../../img/configuration_2x.png" alt="Configuration"/>
</div>

<div class="icons">
  <div>
    <pre>
      <code class="hljs css">
        <span class="hljs-comment">/* [ Active ] */</span>
        <span class="hljs-rule">
          <span class="hljs-attribute">color</span>
          <span class="hljs-rule">:</span>
          <span class="hljs-value">
            <span class="hljs-hexcolor"> #A1A1A1</span>
          </span>
        </span>;
      </code>
    </pre>
  </div>
  <img src="../../../../img/dashboard-on_2x.png" alt="Dashboard"/>
  <img src="../../../../img/user-group-on_2x.png" alt="Users"/>
  <img src="../../../../img/system-on_2x.png" alt="System"/>
  <img src="../../../../img/servers-on_2x.png" alt="Servers"/>
  <img src="../../../../img/events-on_2x.png" alt="Events"/>
  <img src="../../../../img/recordings-on_2x.png" alt="Recordings"/>
  <img src="../../../../img/configuration-on_2x.png" alt="Configuration"/>
</div>
<br/>

<!-- #### UI Control Icons -->

#### Country Icons
Country icons are located at the top-right of the interface
beside the user dropdown to indicate language that the UI is
represented in.

Each flag should:

* be public domain
* be an svg image
* have an aspect ratio of 3:2
* be named according to the Crowdin languages
* be without additional decoration (official flags)

A good source for these flags are the national flag articles of Wikipedia.<br/>E.g.
[https://en.wikipedia.org/wiki/Flag_of_Germany](https://en.wikipedia.org/wiki/Flag_of_Germany)

<div class="icons flags">
  <img src="../../../../img/lang/de_DE.svg" alt="de_DE"/>
  <img src="../../../../img/lang/el_GR.svg" alt="el_GR"/>
  <img src="../../../../img/lang/en_US.svg" alt="en_US"/>
  <img src="../../../../img/lang/es_ES.svg" alt="es_ES"/>
  <img src="../../../../img/lang/fr_FR.svg" alt="fr_FR"/>
  <img src="../../../../img/lang/gl_ES.svg" alt="gl_ES"/>
  <img src="../../../../img/lang/ja_JP.svg" alt="ja_JP"/>
  <img src="../../../../img/lang/nl_NL.svg" alt="nl_NL"/>
  <img src="../../../../img/lang/no_NO.svg" alt="no_NO"/>
  <img src="../../../../img/lang/pl_PL.svg" alt="pl_PL"/>
  <img src="../../../../img/lang/sv_SE.svg" alt="sv_SE"/>
  <img src="../../../../img/lang/zh_CN.svg" alt="zh_CN"/>
  <img src="../../../../img/lang/zh_TW.svg" alt="zh_TW"/>
</div>

```css
.nav-dd-container .lang img {
    border: 1px solid gray;
    border-radius: 1px;
    height: 18px;
    vertical-align: middle;
}
```
