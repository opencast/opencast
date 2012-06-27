#! /usr/bin/env python

######################
# Felix Watch Script #
######################

def main():
  """MH-4493: This script will periodically check to see if a capture agent
  is running and, if not, starts the capture agent and send an email
  """
  import datetime
  import os
  import subprocess
  import sys
  
  # check it Felix is running first. If it is, there is nothing to do
  processes = subprocess.Popen(["ps", "aux"], stdout=subprocess.PIPE).communicate()[0]
  if processes.find("felix.jar") != -1:
    print "Felix is already running."
    return 0
    
  # start felix
  felix_home = os.environ["FELIX_HOME"]
  os.spawnl(os.P_NOWAIT, os.path.join(felix_home, "bin/start_matterhorn.sh &"))

  # read in the required properties
  config = os.path.join(felix_home, "conf/services/org.opencastproject.capture.impl.ConfigurationManager.properties")
  configFile = file(config, "r")
  properties = dict()
  for line in configFile:
    definition = line.strip()
    if len(definition) == 0:
      continue
    if definition[0] in ('!', '#'):
      continue
    name,prop = definition.split("=")
    properties[name] = prop
    
  # current time
  now = datetime.datetime.now()  
  
  # identify necessary properties  
  recipients = properties["capture.error.emails"].split(",")
  server = properties["capture.error.smtp"]
  user = properties["capture.error.smtp.user"]
  passwd = properties["capture.error.smtp.password"]
  subject = properties["capture.error.subject"].replace("%hostname", os.uname()[1]).replace("%date", now.strftime("%c")).strip("\"")
  messagebody = properties["capture.error.messagebody"].replace("%hostname", os.uname()[1]).replace("%date", now.strftime("%c")).strip("\"")
  
  if user == "":
    user = os.uname()[1]
  if server == "":
    server = "localhost"
  
  # write the message to stderr
  sys.stderr.write(subject + ": " + messagebody + "\n")
  
  # send the message to each of the mail recipients
  for recipient in recipients:
    mail(user, server, recipient, subject, messagebody)

def mail(sourceEmail, sourceServer, destinationEmail, subject, contents):
	"""Sends mail from sourceEmail using sourceServer to destinationEmail with the
	subject and contents being set by the last two arguments respectively
	
	@type sourceEmail:  Email Address
	@param sourceEmail:  The email address to send from
	@type sourceServer:  Server name or IP
	@param sourceServer:  The email server to send with
	@type destinationEmail:  Email Address
	@param destinationEmail:  The email address to send to
	@type subject:  String
	@param subject:  The subject for the email
	@type contents:  String
	@param contents:  The conten for the email
	"""
	import smtplib

	source = sourceEmail + "@" + sourceServer
	msg = """From: %s\nTo: %s\nSubject: %s\n\n%s""" % (source, destinationEmail, subject, contents)
	print msg
	# send the message
	server = smtplib.SMTP(sourceServer)
	smtpresult = server.sendmail(source, destinationEmail, msg)
	server.quit()
	if smtpresult:
		errstr = ""
		for recip in smtpresult.keys():
			errstr = """Could not delivery mail to: %s\nServer said: %s\n%s\n\n%s""" % (recip, smtpresult[recip][0], smtpresult[recip][1], errstr)
			print errstr
	return 0

if __name__ == "__main__":
  main()
