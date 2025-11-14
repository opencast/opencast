window.addEventListener("load", async () => {
  const search = window.location.search.slice(1).split("&").reduce<Record<string, string>>((acc, param) => {
    if (param) {
      const [key, value] = param.split(/=(.*)/);    
      acc[key] = value ? decodeURIComponent(value) : '';
    }
    return acc;
  }, {});
  

  const redirect = search['redirect'];
  const id = search['id'];
  const series = search['series'] || search['sid'] || search['epFrom'];

  if (redirect !== undefined) {    
    const redirectDomain = new URL(redirect);
    if (window.location.hostname.toUpperCase() === redirectDomain.hostname.toUpperCase()) {
      window.location.href = redirect;
    }
    else {
      window.location.href = "/engage/ui/index.html";
    }
  }
  else if (id !== undefined) {
    window.location.href = "watch.html?id=" + id;
  }
  else if (series !== undefined) {
    window.location.href = "/engage/ui/index.html?epFrom=" + series;
  }
  else {
    window.location.href = "/engage/ui/index.html";
  }
});
