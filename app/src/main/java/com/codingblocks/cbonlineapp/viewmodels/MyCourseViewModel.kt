package com.codingblocks.cbonlineapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.codingblocks.cbonlineapp.database.ContentDao
import com.codingblocks.cbonlineapp.database.CourseRunDao
import com.codingblocks.cbonlineapp.database.SectionDao
import com.codingblocks.cbonlineapp.database.SectionWithContentsDao
import com.codingblocks.cbonlineapp.database.models.ContentCodeChallenge
import com.codingblocks.cbonlineapp.database.models.ContentCsvModel
import com.codingblocks.cbonlineapp.database.models.ContentDocument
import com.codingblocks.cbonlineapp.database.models.ContentLecture
import com.codingblocks.cbonlineapp.database.models.ContentQna
import com.codingblocks.cbonlineapp.database.models.ContentVideo
import com.codingblocks.cbonlineapp.database.models.CourseContent
import com.codingblocks.cbonlineapp.database.models.CourseSection
import com.codingblocks.cbonlineapp.database.models.SectionWithContent
import com.codingblocks.cbonlineapp.extensions.getDistinct
import com.codingblocks.cbonlineapp.extensions.retrofitCallback
import com.codingblocks.cbonlineapp.util.SingleLiveEvent
import com.codingblocks.onlineapi.Clients
import com.codingblocks.onlineapi.models.ProductExtensionsItem
import com.codingblocks.onlineapi.models.ResetRunAttempt

class MyCourseViewModel(
    private val runDao: CourseRunDao,
    private val sectionWithContentsDao: SectionWithContentsDao,
    private val contentsDao: ContentDao,
    private val sectionDao: SectionDao
) : ViewModel() {

    var progress: MutableLiveData<Boolean> = MutableLiveData()
    var revoked: MutableLiveData<Boolean> = MutableLiveData()
    var attemptId: String = ""
    var runId: String = ""
    var courseId: String = ""
    private val mutablePopMessage = SingleLiveEvent<String>()
    val popMessage: LiveData<String> = mutablePopMessage
    var resetProgres: MutableLiveData<Boolean> = MutableLiveData()

    private val extensions = MutableLiveData<List<ProductExtensionsItem>>()

    fun updatehit(attemptId: String) {
        runDao.updateHit(attemptId)
    }

    fun getResumeCourse() = sectionWithContentsDao.resumeCourse(attemptId)

    fun getRunAttempt(runId: String): String = runDao.getRunByRunId(runId).crAttemptId

    fun getContentWithSectionId(id: String) = sectionWithContentsDao.getContentWithSectionId(id).getDistinct()

    fun getSectionDownloadlist(id: String) = sectionWithContentsDao.getVideoIdsWithSectionId(id)

    fun updateContent(id: String, lectureContentId: String, s: String) = contentsDao.updateContent(id, lectureContentId, s)

    fun updateProgressLecture(sectionId: String, contentId: String, s: String, s1: String) = contentsDao.updateProgressLecture(sectionId, contentId, s, s1)

    fun updateProgressDocument(sectionId: String, contentId: String, s: String, s1: String) = contentsDao.updateProgressDocument(sectionId, contentId, s, s1)

    fun updateProgressVideo(sectionId: String, contentId: String, s: String, s1: String) = contentsDao.updateProgressVideo(sectionId, contentId, s, s1)

    fun updateProgressQna(sectionId: String, contentId: String, s: String, s1: String) = contentsDao.updateProgressQna(sectionId, contentId, s, s1)

    fun getCourseSection(attemptId: String) = sectionDao.getCourseSection(attemptId)

    fun getRunByAtemptId(attemptId: String) = runDao.getRunByAtemptId(attemptId)

    fun fetchCourse(attemptId: String) {
        Clients.onlineV2JsonApi.enrolledCourseById(attemptId)
            .enqueue(retrofitCallback { _, response ->
                response?.let { runAttempt ->
                    if (runAttempt.isSuccessful) {
                        runAttempt.body()?.run?.sections?.let { sectionList ->
                            sectionList.forEach { courseSection ->
                                courseSection.run {
                                    val newSection = CourseSection(
                                        id, name,
                                        order, premium, status,
                                        runId, attemptId, updatedAt
                                    )
                                    val oldSection = sectionDao.getSectionWithId(id)
                                    if (oldSection == null)
                                        sectionDao.insert(newSection)
                                    else if (oldSection == newSection) {
                                        sectionDao.update(newSection)
                                    }
                                    courseContentLinks?.related?.href?.substring(7)
                                        ?.let { contentLink ->
                                            Clients.onlineV2JsonApi.getSectionContents(
                                                contentLink
                                            )
                                                .enqueue(
                                                    retrofitCallback { _, contentResponse ->
                                                        contentResponse?.let { contentList ->
                                                            if (contentList.isSuccessful) {
                                                                contentList.body()
                                                                    ?.forEach { content ->
                                                                        var contentDocument =
                                                                            ContentDocument()
                                                                        var contentLecture =
                                                                            ContentLecture()
                                                                        var contentVideo =
                                                                            ContentVideo()
                                                                        var contentQna =
                                                                            ContentQna()
                                                                        var contentCodeChallenge =
                                                                            ContentCodeChallenge()
                                                                        var contentCsv =
                                                                            ContentCsvModel()

                                                                        when {
                                                                            content.contentable == "lecture" -> content.lecture?.let { contentLectureType ->
                                                                                contentLecture =
                                                                                    ContentLecture(
                                                                                        contentLectureType.id,
                                                                                        contentLectureType.name
                                                                                            ?: "",
                                                                                        contentLectureType.duration
                                                                                            ?: 0,
                                                                                        contentLectureType.videoId
                                                                                            ?: "",
                                                                                        content.sectionContent?.id
                                                                                            ?: "",
                                                                                        contentLectureType.updatedAt
                                                                                    )
                                                                            }
                                                                            content.contentable == "document" -> content.document?.let { contentDocumentType ->
                                                                                contentDocument =
                                                                                    ContentDocument(
                                                                                        contentDocumentType.id,
                                                                                        contentDocumentType.name
                                                                                            ?: "",
                                                                                        contentDocumentType.pdfLink
                                                                                            ?: "",
                                                                                        content.sectionContent?.id
                                                                                            ?: "",
                                                                                        contentDocumentType.updatedAt
                                                                                    )
                                                                            }
                                                                            content.contentable == "video" -> content.video?.let { contentVideoType ->
                                                                                contentVideo =
                                                                                    ContentVideo(
                                                                                        contentVideoType.id,
                                                                                        contentVideoType.name
                                                                                            ?: "",
                                                                                        contentVideoType.duration
                                                                                            ?: 0L,
                                                                                        contentVideoType.description
                                                                                            ?: "",
                                                                                        contentVideoType.url
                                                                                            ?: "",
                                                                                        content.sectionContent?.id
                                                                                            ?: "",
                                                                                        contentVideoType.updatedAt
                                                                                    )
                                                                            }
                                                                            content.contentable == "qna" -> content.qna?.let { contentQna1 ->
                                                                                contentQna =
                                                                                    ContentQna(
                                                                                        contentQna1.id,
                                                                                        contentQna1.name
                                                                                            ?: "",
                                                                                        contentQna1.qId
                                                                                            ?: 0,
                                                                                        content.sectionContent?.id
                                                                                            ?: "",
                                                                                        contentQna1.updatedAt
                                                                                    )
                                                                            }
                                                                            content.contentable == "code_challenge" -> content.codeChallenge?.let { codeChallenge ->
                                                                                contentCodeChallenge =
                                                                                    ContentCodeChallenge(
                                                                                        codeChallenge.id,
                                                                                        codeChallenge.name
                                                                                            ?: "",
                                                                                        codeChallenge.hbProblemId
                                                                                            ?: 0,
                                                                                        codeChallenge.hbContestId
                                                                                            ?: 0,
                                                                                        content.sectionContent?.id
                                                                                            ?: "",
                                                                                        codeChallenge.updatedAt
                                                                                    )
                                                                            }
                                                                            content.contentable == "csv" -> content.csv?.let {
                                                                                contentCsv =
                                                                                    ContentCsvModel(
                                                                                        it.id,
                                                                                        it.name
                                                                                            ?: "",
                                                                                        it.description
                                                                                            ?: "",
                                                                                        it.contentId
                                                                                            ?: "",
                                                                                        it.updatedAt
                                                                                    )
                                                                            }
                                                                        }

                                                                        var progressId = ""
                                                                        val status: String
                                                                        if (content.progress != null) {
                                                                            status =
                                                                                content.progress?.status
                                                                                    ?: ""
                                                                            progressId =
                                                                                content.progress?.id
                                                                                    ?: ""
                                                                        } else {
                                                                            status =
                                                                                "UNDONE"
                                                                        }

                                                                        val newContent =
                                                                            CourseContent(
                                                                                content.id,
                                                                                status,
                                                                                progressId,
                                                                                content.title,
                                                                                content.duration
                                                                                    ?: 0,
                                                                                content.contentable,
                                                                                content.sectionContent?.order
                                                                                    ?: 0,
                                                                                content.sectionContent?.sectionId
                                                                                    ?: "",
                                                                                attemptId,
                                                                                courseSection.premium,
                                                                                content.sectionContent?.updatedAt
                                                                                    ?: "",
                                                                                contentLecture,
                                                                                contentDocument,
                                                                                contentVideo,
                                                                                contentQna,
                                                                                contentCodeChallenge,
                                                                                contentCsv
                                                                            )

                                                                        val oldContent =
                                                                            contentsDao.getContentWithId(
                                                                                attemptId,
                                                                                content.id
                                                                            )
                                                                        if (oldContent == null) {

                                                                            contentsDao.insert(
                                                                                newContent
                                                                            )
                                                                            sectionWithContentsDao.insert(
                                                                                SectionWithContent(
                                                                                    courseSection.id,
                                                                                    content.id,
                                                                                    content.sectionContent?.order
                                                                                        ?: 0

                                                                                )
                                                                            )
                                                                        } else if (oldContent != newContent) {
                                                                            contentLecture.isDownloaded =
                                                                                oldContent.contentLecture.isDownloaded
                                                                            contentsDao.update(
                                                                                newContent
                                                                            )
                                                                        }
                                                                    }
                                                            }
                                                        }
                                                    })
                                        }
                                }
                            }
                        }
                        progress.value = false
                    } else if (runAttempt.code() == 404) {
                        revoked.value = true
                    }
                }
            })

//            try {
//                sectionWithContentsDao.insert(
//                    SectionWithContent(
//                        sectionId,
//                        contentId
//                    )
//                )
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e("CRASH", "COURSE ID : $sectionId")
//                Log.e("CRASH", "INSTRUCTOR ID : $contentId")
//            }
//        }
//    }
    }

    fun resetProgress() {
        val resetCourse = ResetRunAttempt(attemptId)
        Clients.api.resetProgress(resetCourse).enqueue(retrofitCallback { _, response ->
            resetProgres.value = response?.isSuccessful ?: false
        })
    }

    fun requestApproval() {
        Clients.api.requestApproval(attemptId).enqueue(retrofitCallback { throwable, response ->
            response.let {
                if (it?.isSuccessful == true) {
                    mutablePopMessage.value = it.body()?.string()
                } else {
                    mutablePopMessage.value = it?.errorBody()?.string()
                }
            }
            throwable.let {
                mutablePopMessage.value = it?.message
            }
        })
    }

    fun fetchExtensions(productId: Int): MutableLiveData<List<ProductExtensionsItem>> {
        Clients.api.getExtensions(productId).enqueue(retrofitCallback { throwable, response ->
            response?.body().let { list ->
                if (response?.isSuccessful == true) {
                    extensions.postValue(list?.productExtensions)
                }
            }
            throwable.let {
                mutablePopMessage.value = it?.message
            }
        })
        return extensions
    }
}
