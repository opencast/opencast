	var request = new requestHandler();
	var message = new messageView();

	function restartSystem() {
		
		var text = "This action will shut down the Matterhorn server! \n "
		text += "Please start your Matterhorn server manually: \n ";
		text += "1. login on your Matterhorn server \n ";
		text += "2. type \"sudo service matterhorn restart\"";
		message.createRestartView(text);
	}
	
	function handleRestart() {
		message.closeRestartView();
		request.sendPostRequestWithPluginData("restart", "restart", "restart", "restart");
	}
	
	function closeRestartMessage() {
		message.closeRestartView();
	}