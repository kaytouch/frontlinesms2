<%@ page contentType="text/html;charset=UTF-8" %>
<div id="contacts-list">
	<g:if test="${contactInstanceTotal > 0}">
		<ol id="contact-list">
			<g:if test="${!contactInstance ? false : !contactInstance.id}">
				<li class="selected" id="newContact">
					<g:checkBox disabled="disabled" name='new-contact-select' />
					<a disabled="disabled" href="">[New Contact]</a>
				</li>
			</g:if>
			<g:each in="${contactInstanceList}" status="i" var="c">
				<li class="${c.id == contactInstance?.id ? 'selected' : ''}" id="contact-${c.id}">
					<g:checkBox name="contact-select" class="contact-select" id="contact-select-${c.id}"
							checked="${params.checkedId == c.id + '' ? 'true': 'false'}" value="${c.id}" onclick="contactChecked(${c.id});"/>
					<g:if test="${contactsSection instanceof frontlinesms2.Group}">
						<g:set var="contactLinkParams" value="[groupId:contactsSection.id]"/>
					</g:if>
					<g:elseif test="${contactsSection instanceof frontlinesms2.SmartGroup}">
						<g:set var="contactLinkParams" value="[smartGroupId:contactsSection.id]"/>
					</g:elseif>
					<g:else><g:set var="contactLinkParams" value="[]"/></g:else>
					<g:link class="displayName-${c.id}" action="show" params="${contactLinkParams + [contactId:c.id, sort:params.sort, offset:params.offset]}">
						${c.name?:c.primaryMobile?:c.secondaryMobile?:'[No Name]'}
					</g:link>
				</li>
			</g:each>
		</ol>
	</g:if>
	<g:else>
		<div id="contact-list">
			No contacts here!
		</div>
	</g:else>
</div>
