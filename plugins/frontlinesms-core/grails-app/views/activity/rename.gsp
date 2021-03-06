<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="frontlinesms2.Activity" %>
<div>
	<div class="error-panel hide" id="smallpopup-error-panel"><div id="error-icon"></div></div>
	<g:formRemote name="rename-activity" url="[action:'update']" method="post" onSuccess="smallPopup.checkResults(data)">
		<g:hiddenField name="id" value="${params.ownerId}"></g:hiddenField>
		<div class="dialog">
			<table>
				<tbody>
					<tr class="prop">
						<td valign="top" class="name">
							<label class="bold inline" for="title"><g:message code="activity.name"/></label>
						</td>
						<td valign="top" class="value">
							<g:textField name="name" value="${Activity.get(params.ownerId)?.name}"/>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</g:formRemote>
</div>
