function askPermission() {
  return new Promise((resolve, reject) => {
    const permissionRessult = Notification.requestPermission(result => {
      resolve(result);
    });

    if (permissionResult) {
      permissionResult.then(resolve, reject);
    }
  })
  .then(permissionResult => {
    if (permissionResult !== 'granted') {
      throw new Error('no permission given');
    }
  });
}
