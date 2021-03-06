/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Association du Paris Java User Group.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package controllers

import models.{ConferenceDescriptor, ArchiveProposal}
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.Future

/**
 * Attic service to archive conference and talks.
 * @author created by N.Martignole, Innoteria, on 14/11/2014.
 */
object Attic extends SecureCFPController {

  def atticHome() = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.Attic.atticHome())
  }

  val opTypeForm = Form("opType" -> text)

  /**
   * Either destroy [draft] or [deleted] proposals, using a Future, as this code is slow and blocks
   * @return
   */
  def prune() = SecuredAction(IsMemberOf("admin")).async {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      import scala.concurrent.ExecutionContext.Implicits.global

      opTypeForm.bindFromRequest().fold(hasErrors => Future.successful(BadRequest(views.html.Attic.atticHome())),
        (opType: String) => opType match {
          case o if o == "deleted" =>
            val futureTotalDeleted = Future(ArchiveProposal.pruneAllDeleted())
            futureTotalDeleted.map { totalDeleted =>
              Redirect(routes.Attic.atticHome()).flashing(("success", s"$totalDeleted Deleted moved to Attic"))
            }
          case o if o == "draft" =>
            val futureTotalDraft = Future(ArchiveProposal.pruneAllDraft())
            futureTotalDraft.map {
              totalDraft =>
                Redirect(routes.Attic.atticHome()).flashing(("success", s"$totalDraft Draft deleted"))
            }
          case other =>
            Future.successful(Redirect(routes.Attic.atticHome()))
        }
      )
  }


  val proposalTypeForm = Form("proposalType" -> text)

  def doArchive() = SecuredAction(IsMemberOf("admin")).async {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      import scala.concurrent.ExecutionContext.Implicits.global
      proposalTypeForm.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(views.html.Attic.atticHome())),
        proposalType => {
          Future(ArchiveProposal.archiveAll(proposalType)).map{
            totalArchived =>
               Redirect(routes.Attic.atticHome()).flashing(("success", s"$totalArchived archived"))
          }
        }
      )
  }


}
