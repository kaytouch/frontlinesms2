import frontlinesms2.*
import groovyx.remote.transport.http.HttpTransport
import groovyx.remote.client.RemoteControl

class MigrationSpec {
	static boolean gitWorkingDirectoryMustBeClean = true

	String serverPort

	public static void main(String... args) {
		init()
		gitWorkingDirectoryMustBeClean = !('--dirty' in args)
		def exitCode = 0
		def originalGitBranch = executeGetText('git branch | grep '*' | cut -d" " -f2')
		try {
			new MigrationSpec(serverPort:8080).test()
		} catch(Exception ex) {
			ex.printStackTrace()
			exitCode = 1
		} finally {
			try {
				simpleExecute('cd ../../../.. && git reset --hard && git clean -xdf')
			} catch(Exception _) { _.printStackTrace() }
			try {
				simpleExecute("git checkout $originalGitBranch")
			} catch(Exception _) { _.printStackTrace() }
		}
		System.exit(exitCode)
	}

	private String getServerAddress(String contextPath) {
		"http://localhost:$serverPort/$contextPath/grails-remote-control"
	}

	private static void init() {
		// TODO fail if git working directory not clean

		if(gitWorkingDirectoryMustBeClean) {
			def porcelainOutput = simpleExecute('git status --porcelain | /usr/bin/env grep --quiet "."')
			println "# porcelainOutput=$porcelainOutput"
			def clean = porcelainOutput == 1
			println "# Git working directory is clean? $clean"
			if(!clean) {
				throw new RuntimeException("GIT WORKING DIRECTORY IS NOT CLEAN.  TERMINATING.")
			}
		}
	}

	private static String executeGetText(String command) {
		return executeInBackground(command).text
	}

	private static Process executeInBackground(String command) {
		println "# Executing command: $command"
		return ['bash', '-c', command].execute()
	}

	private static int simpleExecute(String command) {
		return executeInBackground(command).waitFor()
	}

	private static int execute(String command, String errorMessage=null, boolean throwExceptionOnFailure=true) {
		def exitCode = simpleExecute(command)
		if(throwExceptionOnFailure && exitCode) {
			throw new RuntimeException(errorMessage?: "Command failed: $command; exit code: $exitCode")
		}
		return exitCode as Integer
	}

	def getRemoteControl = { contextPath ->
		def transport = new HttpTransport(getServerAddress(contextPath))
		return new RemoteControl(transport)
	}

	// TEST HELPERS
	def withFrontlineSMS = { String version, contextPath = 'frontlinesms-core', Closure remoteCode ->
		def gitTag = (version ==~  /\d+(\.\d+)*/)? "frontlinesms$version": version

		println "# Checking out FrontlineSMS version: $version..."
		execute("git checkout $gitTag")
		execute(/sed -i -E -e "s:^.*remote-control.*\$::" -e "s\/plugins\s*\{\/plugins {\\ncompile \":remote-control:1.3\"\/" / + "../../../$contextPath/grails-app/conf/BuildConfig.groovy")

		println "# Starting grails server on port $serverPort..."
		def grailsServer = executeInBackground "cd ../../../$contextPath && grails -Dserver.port=$serverPort test run-app"
		println "# Waiting for grails server to start..."
		try {
			grailsServer.inputStream.eachLine { line ->
				println "# [grails] $line"
				if(line.trim().startsWith('| Server running. Browse to ')) {
					throw new EOFException('Server started successfully.')
				}
			}
		} catch(EOFException _) {}

		println "# Running test script with remote control..."
		def remoteControl = getRemoteControl(contextPath)
		def testOutput = remoteControl.exec(remoteCode)

		println "# Killing remote server"
		grailsServer.destroy()

		println "# Checking test response code..."
		println "# Test output: $testOutput"
		// TODO handle errors and finally cleanup
	}

	def withCurrentFrontlineSMS = { Closure remoteCode ->
		println "# Starting grails server on port $serverPort..."
		// TODO start server
		println "# Running test script with remote control..."
		// TODO run test script on remote server
	}

	def performMigration = { Closure remoteCode ->
		println "# TODO run migration..."
	}

	def test() {
		withFrontlineSMS('2.1.3', 'core') {
			new ClickatellFconnection(name:"Test Clickatell connection", apiId: "doesntmatter", username:"testuser", password:"testpass").save(failOnError:true)
			def keyword = new Keyword(value: 'FOOTBALL')
			def poll1 = new Poll(name: 'Football Teams', question:"Who will win?", sentMessageText:"Who will win? Reply FOOTBALL A for 'manchester' or FOOTBALL B for 'barcelona'", autoreplyText:'Thank you, ${contact_name}, for participating in the football poll', keyword: keyword)
			poll1.addToResponses(key:'A', value:'manchester', aliases:'MANCHESTER, A')
			poll1.addToResponses(key:'B', value:'barcelona', aliases:'BARCELONA, B')
			poll1.addToResponses(PollResponse.createUnknown())
				
			poll1.save(failOnError:true, flush:true)
			PollResponse.findByValue('manchester').addToMessages(new Fmessage(src:'+123', date:new Date(), text:'UTD!'))
			PollResponse.findByValue('manchester').addToMessages(new Fmessage(src:'+123', date:new Date(), text:'MUFC!'))
			PollResponse.findByValue('unknown').addToMessages(new Fmessage(src:'+123', date:new Date(), text:'All I want is a good game.'))

			def barcelonaResponse = PollResponse.findByValue('barcelona');
			10.times {
				def msg = new Fmessage(src: "+9198765432${it}", date: new Date() - it, text: "Barca", inbound:true)
				msg.save(failOnError: true);
				barcelonaResponse.addToMessages(msg);
			}
			poll1.save(flush: true)
			def poll2 = new Poll(name: 'No keywords', question:"Are keywords mandatory?", sentMessageText:"Must I use keywords? Reply if you want, but automatic sorting is disabled", autoreplyText:"You may be right", yesNo:true)
			poll2.addToResponses(key:'A', value:'yes', aliases:'SOMETHING, IRRELEVANT')
			poll2.addToResponses(key:'B', value:'no')
			poll2.addToResponses(PollResponse.createUnknown())		
			poll2.save(failOnError:true, flush:true)
			new Autoreply(name:"Toothpaste", keyword: new Keyword(value: 'MENO'), autoreplyText: "Thanks for the input. Your number, ${contact_number}, has been added to our records").save(failOnError:true, flush:true)
		}

		withFrontlineSMS('2.2.0') {
			// check data migrated properly
			def click = ClickatellFconnection.findByName("Test Clickatell connection")
			assert click.apiId == "doesntmatter"
			assert click.username == "testuser"
			assert click.password == "testpass"
			assert click.sendToUsa == false
			assert click.fromNumber == null

			def poll1 = Poll.findByName('Football Teams')
			assert poll1.question == "Who will win?"
			assert poll1.sentMessageText == "Who will win? Reply FOOTBALL A for 'manchester' or FOOTBALL B for 'barcelona'"
			assert poll1.autoreplyText == "Thank you, ${recipient_name}, for participating in the football poll"
			assert poll1.keywords*.value.sort() == ['']
			assert poll1.keywords.size() == 5
			assert poll1.keywords*.value.sort() == ['A', 'B', 'BARCELONA', 'FOOTBALL', 'MANCHESTER']
			['A':false, 'B':false, 'BARCELONA':false, 'FOOTBALL':true, 'MANCHESTER':false].each { k, v ->
				assert Keyword.findByValue(k).isTopLevel == v
			}

			def poll2 = Poll.findByName("No keywords")
			assert poll2.keywords.size() == 0
			assert !Keyword.findByValue('SOMETHING')
			assert !Keyword.findByValue('IRRELEVANT')

			def autoreply = Autoreply.findByName('Toothpaste')
			assert autoreply.autoreplyText == "Thanks for the input. Your number, ${recipient_number}, has been added to our records"

			// create any additional data for future
		}

		performMigration {
			// some migration you're trying to test out...
		}

		withCurrentFrontlineSMS {
			// check that the `performMigration` executed OK
		}
	}
}

