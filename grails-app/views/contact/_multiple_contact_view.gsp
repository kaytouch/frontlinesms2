<div class="multiple-contact hide">
	<div id="action-buttons" class="buttons">
			<g:actionSubmit class="btn" id="update-all" action="updateMultipleContacts" value="Save All" disabled="disabled"/>
			<g:link class="cancel btn" disabled="disabled">Cancel</g:link>
			<a class="btn" id="btn_delete_all" onclick="launchConfirmationPopup('Delete all');">
				Delete all
			</a>
	</div>
	<div class="basic-info">
		<div id="contact-count">&nbsp;</div>
	</div>
	<div class="multiple-contact">
		<div>
			<ol id='multi-group-list'>
				<g:each in="${sharedGroupInstanceList}" status="i" var="g">
					<li id="${g.name}" class="${g == groupInstance ? 'selected' : ''}">
						<span>${g.name}</span>
						<a class="remove-group" id="remove-group-${g.id}"><img class='remove' src='${resource(dir:'images/icons',file:'remove.png')}' /></a>
					</li>
				</g:each>
			</ol>
		</div>
		<div id='multi-group-add' class="dropdown">
			<g:select name="multi-group-dropdown"
					noSelection="['_':'Add to group...']"
					from="${nonSharedGroupInstanceList}"
					optionKey="id"
					optionValue="name"/>
		</div>
	</div>
</div>
