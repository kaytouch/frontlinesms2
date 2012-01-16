package frontlinesms2

import grails.util.GrailsConfig
import grails.converters.*
import java.lang.*

class MessageController {
	
	static allowedMethods = [save: "POST", update: "POST",
			delete: "POST", deleteAll: "POST",
			archive: "POST", archiveAll: "POST"]

	def messageSendService
	def fmessageInfoService
	def trashService
	def newMessagesService

	def bobInterceptor = {
		params.offset  = params.offset ?: 0
		params.max = params.max ?: GrailsConfig.config.grails.views.pagination.max
		if(params.action == sent || params.action == pending) params.sort = params.sort ?: 'dateSent'
		else params.sort = params.sort ?: 'dateReceived'
		params.order = params.order ?: 'desc'
		params.viewingArchive = params.viewingArchive ? params.viewingArchive.toBoolean() : false
		params.starred = params.starred ? params.starred.toBoolean() : false
		params.failed = params.failed ? params.failed.toBoolean() : false
		Fmessage.withNewSession { session ->
			Fmessage.findAll().each {
				if(it.inbound && Contact.findByPrimaryMobile(it.src))
					Fmessage.executeUpdate("UPDATE Fmessage m SET m.contactName=?,m.contactExists=? WHERE m.src=?", [Contact.findByPrimaryMobile(it.src).name, true, it.src])
				else if(!it.inbound && Contact.findByPrimaryMobile(it.dst))
					Fmessage.executeUpdate("UPDATE Fmessage m SET m.contactName=?,m.contactExists=? WHERE m.dst=?", [Contact.findByPrimaryMobile(it.dst).name, true, it.dst])
				else if(it.contactExists) {
					it.contactName = ''
					it.contactExists = false
				}
			}
		}
	}
	def beforeInterceptor = bobInterceptor
	
	def index = {
		params.sort = 'dateReceived'
		redirect(action:'inbox', params:params)
	}
	
	def getNewMessageCount = {
		def section = params.messageSection
		if(!params.ownerId && section != 'trash') {
			def messageCount = [totalMessages:[Fmessage."$section"().count()]]
			render messageCount as JSON
		} else if(section == 'poll') {
			def messageCount = [totalMessages:[Poll.get(params.ownerId)?.getPollMessages().count()]]
			render messageCount as JSON
		} else if(section == 'announcement') {
			def messageCount = [totalMessages:[Announcement.get(params.ownerId)?.getAnnouncementMessages().count()]]
			render messageCount as JSON
		} else if(section == 'folder') {
			def messageCount = [totalMessages:[Folder.get(params.ownerId)?.getFolderMessages().count()]]
			render messageCount as JSON
		} else
			render ""
	}
	
	def getShowModel(messageInstanceList) {
		def messageInstance = (params.messageId) ? Fmessage.get(params.messageId) : messageInstanceList ? messageInstanceList[0]:null
		if (messageInstance && !messageInstance.read) {
			messageInstance.read = true
			messageInstance.save()
		}
		def responseInstance = messageInstance?.messageOwner
		def checkedMessageCount = params.checkedMessageList?.tokenize(',')?.size()
		def selectedMessageList = params.checkedMessageList?: ',' + messageInstance?.id + ','
		[messageInstance: messageInstance,
				checkedMessageCount: checkedMessageCount,
				checkedMessageList: selectedMessageList,
				folderInstanceList: Folder.findAllByArchivedAndDeleted(params.viewingArchive, false),
				responseInstance: responseInstance,
				pollInstanceList: Poll.findAllByArchivedAndDeleted(params.viewingArchive, false),
				announcementInstanceList: Announcement.findAllByArchivedAndDeleted(params.viewingArchive, false),
				messageCount: Fmessage.countAllMessages(params),
				hasFailedMessages: Fmessage.hasFailedMessages(),
				viewingArchvive: params.viewingArchive]
	}

	def inbox = {
		def messageInstanceList = Fmessage.inbox(params.starred, params.viewingArchive)
		render view:'standard', model:[messageInstanceList: messageInstanceList.list(params),
					messageSection: 'inbox',
					messageInstanceTotal: messageInstanceList.count()] << getShowModel()
	}

	def sent = {
		def messageInstanceList = Fmessage.sent(params.starred, params.viewingArchive)
		render view:'standard', model:[messageSection: 'sent',
				messageInstanceList: messageInstanceList.list(params),
				messageInstanceTotal: messageInstanceList.count()] << getShowModel()
	}

	def pending = {
		def messageInstanceList = Fmessage.pending(params.failed)
		render view:'standard', model:[messageInstanceList: messageInstanceList.list(params),
				messageSection: 'pending',
				messageInstanceTotal: messageInstanceList.count(),
				failedMessageIds : Fmessage.findHasFailed(true)*.id] << getShowModel()
	}
	
	def trash = {
		def trashInstance
		def trashInstanceList
		def messageInstanceList
		params.sort = params.sort != "dateReceived" ? params.sort : 'dateCreated'
		if(params.id) {
			def setTrashInstance = { obj ->
				if(obj.objectType == "frontlinesms2.Fmessage") {
					params.messageId = obj.linkId
				} else {
					trashInstance = obj.link
				}
			}
			setTrashInstance(Trash.findById(params.id))
		}
		
		if(params.starred) {
			messageInstanceList = Fmessage.deleted(params.starred)
		} else {
			trashInstanceList =  Trash.list(params)
		}
		render view:'standard', model:[trashInstanceList:trashInstanceList,
					messageInstanceList: messageInstanceList?.list(params),
					messageSection: 'trash',
					messageInstanceTotal: Trash.count(),
					ownerInstance: trashInstance] << getShowModel()
	}

	def poll = {
		def pollInstance = Poll.get(params.ownerId)
		def messageInstanceList = pollInstance?.getPollMessages(params.starred)
		
		render view:'../message/poll', model:[messageInstanceList: messageInstanceList?.list(params),
				messageSection: 'poll',
				messageInstanceTotal: messageInstanceList?.count(),
				ownerInstance: pollInstance,
				viewingMessages: params.viewingArchive ? params.viewingMessages : null,
				responseList: pollInstance?.responseStats,
				pollResponse: pollInstance?.responseStats as JSON] << getShowModel()
	}
	
	def announcement = {
		def announcementInstance = Announcement.get(params.ownerId)
		def messageInstanceList = announcementInstance?.getAnnouncementMessages(params.starred)
		if(params.flashMessage) { flash.message = params.flashMessage }
		render view:'../message/standard', model:[messageInstanceList: messageInstanceList?.list(params),
					messageSection: 'announcement',
					messageInstanceTotal: messageInstanceList?.count(),
					ownerInstance: announcementInstance,
					viewingMessages: params.viewingArchive ? params.viewingMessages : null] << getShowModel()
	}
	
	def folder = {
		def folderInstance = Folder.get(params.ownerId)
		def messageInstanceList = folderInstance?.getFolderMessages(params.starred)
		if(params.flashMessage) { flash.message = params.flashMessage }
		render view:'../message/standard', model:[messageInstanceList: messageInstanceList.list(params),
					messageSection: 'folder',
					messageInstanceTotal: messageInstanceList.count(),
					ownerInstance: folderInstance,
					viewingMessages: params.viewingArchive ? params.viewingMessages : null] << getShowModel()
	}

	def send = {
		def failedMessageIds = params.failedMessageIds
		def messages = failedMessageIds ? Fmessage.getAll([failedMessageIds].flatten()): messageSendService.getMessagesToSend(params)
		messages.each { message ->
			messageSendService.send(message)
		}
		flash.message = "Message has been queued to send to " + messages*.dst.join(", ")
		if(params.failedMessageIds)
			redirect(action: pending)
		else
			render(text: flash.message)
		
	}

	def delete = {
		def messageIdList = params.checkedMessageList ? params.checkedMessageList.tokenize(',') : [params.messageId]
		messageIdList.each { id ->
			withFmessage id, {messageInstance ->
				messageInstance.deleted = true
				new Trash(identifier:messageInstance.contactName, message:messageInstance.text, objectType:messageInstance.class.name, linkId:messageInstance.id).save(failOnError: true, flush: true)
				messageInstance.save(failOnError: true, flush: true)
			}
		}
		flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'message.label', default: ''), messageIdList.size() + ' message(s)'])}"
		if(params.messageSection == 'result')
			redirect(controller: 'search', action: 'result', params: [searchId: params.searchId])
		else if(params.viewingArchive)
			redirect(controller:'archive', action: params.messageSection, params: [ownerId: params.ownerId, viewingArchive: params.viewingArchive, starred: params.starred, failed: params.failed])
		else
			redirect(action: params.messageSection, params: [ownerId: params.ownerId, viewingArchive: params.viewingArchive, starred: params.starred, failed: params.failed])
	}
	
	def archive = {
		def messageIdList = params.checkedMessageList ? params.checkedMessageList.tokenize(',') : [params.messageId]
		def listSize = messageIdList.size();
		messageIdList.each { id ->
			withFmessage id, {messageInstance ->
				if(!messageInstance.messageOwner) {
					messageInstance.archived = true
					messageInstance.save(failOnError: true, flush: true)
				} else {
					listSize--
				}
			}
		}
		flash.message = "${message(code: 'default.archived.message', args: [message(code: 'message.label', default: ''), listSize + ' message(s)'])}"
		if(params.messageSection == 'result')
			redirect(controller: 'search', action: 'result', params: [searchId: params.searchId, messageId: params.messageId])
		else
			redirect(controller: 'message', action: params.messageSection, params: [ownerId: params.ownerId])
	}
	
	def unarchive = {
		def messageIdList = params.checkedMessageList ? params.checkedMessageList.tokenize(',') : [params.messageId]
		def listSize = messageIdList.size();
		messageIdList.each { id ->
			withFmessage id, {messageInstance ->
				if(!messageInstance.messageOwner) {
					messageInstance.archived = false
					messageInstance.save(failOnError: true, flush: true)
				} else {
					listSize--
				}
			}
		}
		flash.message = "${message(code: 'default.unarchived.message', args: [message(code: 'message.label', default: ''), listSize + ' message(s)'])}"
		if(params.messageSection == 'result')
			redirect(controller: 'search', action: 'result', params: [searchId: params.searchId, messageId: params.messageId])
		else
			redirect(controller: 'archive', action: params.messageSection, params: [ownerId: params.ownerId])
	}

	def move = {
		def messageIdList = params.messageId.tokenize(',')
		messageIdList.each { id ->
			withFmessage id, {messageInstance ->
				if (messageInstance.deleted == true) messageInstance.deleted = false
				if(Trash.findByLinkId(messageInstance.id)) {
					Trash.findByLinkId(messageInstance.id).delete(flush:true)
				}
				
				if (params.messageSection == 'poll')  {
					def unknownResponse = Poll.get(params.ownerId).responses.find { it.value == 'Unknown'}
					unknownResponse.addToMessages(messageInstance).save()
				} else if (params.messageSection == 'announcement') {
					Announcement.get(params.ownerId).addToMessages(messageInstance).save()
				} else if (params.messageSection == 'folder') {
					Folder.get(params.ownerId).addToMessages(messageInstance).save()
				} else {
					messageInstance.with {
						messageOwner?.removeFromMessages messageInstance
						messageOwner = null
						inbound = true
						messageOwner?.save()
						save()
					}
				}
			}
		}
		flash.message = "${message(code: 'default.updated.message', args: [message(code: 'message.label', default: ''), messageIdList.size() + ' message(s)'])}"
		render ""
	}

	def changeResponse = {
		def messageIdList = params.messageId.tokenize(',')
		messageIdList.each { id ->
			withFmessage id, { messageInstance ->
				def responseInstance = PollResponse.get(params.responseId)
				responseInstance.addToMessages(messageInstance).save(failOnError: true, flush: true)
			}
		}
		flash.message = "${message(code: 'default.updated.message', args: [message(code: 'message.label', default: 'Fmessage'), 'message(s)'])}"
		render ""
	}

	def changeStarStatus = {
		withFmessage { messageInstance ->
			messageInstance.starred =! messageInstance.starred
			messageInstance.save(failOnError: true, flush: true)
			Fmessage.get(params.messageId).messageOwner?.refresh()
            params.remove('messageId')
			render(text: messageInstance.starred ? "starred" : "unstarred")
		}
	}

	def confirmEmptyTrash = { }
	
	def emptyTrash = {
		trashService.emptyTrash()
		redirect(action: 'inbox')
	}
	
	def getUnreadMessageCount = {
		render text: Fmessage.countUnreadMessages(), contentType:'text/plain'
	}
	
	def getSendMessageCount = {	
		def messageInfo
		def message = params.message ?: ''
		if(message)	{ 
			messageInfo = fmessageInfoService.getMessageInfos(message)
			def messageCount = messageInfo.partCount > 1 ? "${messageInfo.partCount} SMS messages": "1 SMS message"
			render text: "Characters remaining ${messageInfo.remaining} ($messageCount)", contentType:'text/plain'
		} else {
			render text: "Characters remaining 160 (1 SMS message)", contentType:'text/plain'
		}
		
	}
	
	private def withFmessage(messageId = params.messageId, Closure c) {
			def m = Fmessage.get(messageId.toLong())
			if(m) c.call(m)
			else render(text: "Could not find message with id ${params.messageId}") // TODO handle error state properly
	}
}
