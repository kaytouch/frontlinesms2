<g:formRemote url="${[action:'save', controller:'poll']}" name='new-poll-form' method="post" onSuccess="launchMediumPopup('Poll created!', data, 'Ok', summaryRedirect)">
	<g:render template="question"/>
	<g:render template="responses"/>
	<g:render template="sorting"/>
	<g:render template="replies"/>
	<g:render template="message"/>
	<div id="tabs-6">
		<g:render template="../quickMessage/select_recipients" model= "['contactList' : contactList,
		                                                                'groupList': groupList,
		                                                                'nonExistingRecipients': [],
		                                                                'recipients': []]"/>
	</div>
	<g:render template="confirm" plugin="${grailsApplication.config.frontlinesms2.plugin}"/>
</g:formRemote>