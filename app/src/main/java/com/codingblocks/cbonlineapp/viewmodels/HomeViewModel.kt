package com.codingblocks.cbonlineapp.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.codingblocks.cbonlineapp.database.CourseDao
import com.codingblocks.cbonlineapp.database.CourseRunDao
import com.codingblocks.cbonlineapp.database.CourseWithInstructorDao
import com.codingblocks.cbonlineapp.database.FeaturesDao
import com.codingblocks.cbonlineapp.database.InstructorDao
import com.codingblocks.cbonlineapp.database.models.Course
import com.codingblocks.cbonlineapp.database.models.CourseFeatures
import com.codingblocks.cbonlineapp.database.models.CourseRun
import com.codingblocks.cbonlineapp.database.models.CourseWithInstructor
import com.codingblocks.cbonlineapp.database.models.Instructor
import com.codingblocks.cbonlineapp.extensions.greater
import com.codingblocks.cbonlineapp.extensions.retrofitCallback
import com.codingblocks.onlineapi.Clients
import com.codingblocks.onlineapi.models.CarouselCards
import com.crashlytics.android.Crashlytics

class HomeViewModel(
    private val courseWithInstructorDao: CourseWithInstructorDao,
    private val courseDao: CourseDao,
    private val runDao: CourseRunDao,
    private val instructorDao: InstructorDao,
    private val featuresDao: FeaturesDao
) : ViewModel() {
    var carouselCards: MutableLiveData<List<CarouselCards>> = MutableLiveData()
    var progress: MutableLiveData<Boolean> = MutableLiveData()

    fun getCourseDao() = courseDao

    fun getCourseWithInstructorDao() = courseWithInstructorDao

    fun getRecommendedRuns() = runDao.getRecommendedRuns()

    fun getCourseById(id: String) = courseDao.getCourse(id)

    fun getAllRuns() = runDao.getAllRuns()

    fun getMyRuns() = runDao.getMyRuns()

    fun getTopRun() = runDao.getTopRun()

    fun fetchRecommendedCourses(recommended: Boolean = true) {
        Clients.onlineV2JsonApi.getRecommendedCourses()
            .enqueue(retrofitCallback { _, response ->
                response?.let {
                    if (response.isSuccessful) {
                        it.body()?.let { courseList ->
                            courseList.forEach { course ->
                                course.run {
                                    courseDao.insertNew(
                                        Course(
                                            id,
                                            title,
                                            subtitle,
                                            logo,
                                            summary,
                                            promoVideo,
                                            difficulty,
                                            reviewCount,
                                            rating,
                                            slug,
                                            coverImage,
                                            updatedAt,
                                            categoryId,
                                            faq
                                        )
                                    )
                                    coursefeatures?.forEach {
                                        featuresDao.insert(CourseFeatures(icon = it.icon, text = it.text, crCourseId = id))
                                    }
                                }
                                var list = course.runs?.filter { run ->
                                    !run.enrollmentStart.greater() && run.enrollmentEnd.greater() && !run.unlisted
                                }?.sortedWith(compareBy { run -> run.price })
                                if (list != null) {
                                    if (list.isEmpty()) {
                                        list =
                                            course.runs?.sortedWith(compareBy { run -> run.price })
                                    }
                                }
                                list?.get(0)?.run {
                                    try {
                                        runDao.insertNew(
                                            CourseRun(
                                                id,
                                                "",
                                                name,
                                                description,
                                                enrollmentStart,
                                                enrollmentEnd,
                                                start,
                                                end,
                                                price,
                                                mrp ?: "",
                                                course.id,
                                                crUpdatedAt = updatedAt,
                                                title = course.title,
                                                recommended = recommended,
                                                summary = course.summary
                                            )
                                        )

                                        course.instructors?.forEach { instructor ->
                                            instructorDao.insertNew(
                                                Instructor(
                                                    instructor.id,
                                                    instructor.name,
                                                    instructor.description ?: "",
                                                    instructor.photo,
                                                    instructor.updatedAt,
                                                    instructor.email,
                                                    instructor.sub
                                                )
                                            )
                                            courseWithInstructorDao.insert(
                                                CourseWithInstructor(course.id, instructor.id)
                                            )
                                        }
                                    } catch (e: Exception) {
                                        progress.value = false
                                    }
                                }
                            }
                        }
                    }
                    progress.value = false
                }
            })
    }

    fun fetchMyCourses(refresh: Boolean = false) {
        try {
            Clients.onlineV2JsonApi.getMyCourses()
                .enqueue(retrofitCallback { _, res ->
                    res?.let { response ->
                        if (response.isSuccessful) {
                            response.body()?.let { courseList ->
                                courseList.forEach { courseRun ->
                                    courseRun.runAttempts?.get(0)?.id?.let { runId ->
                                        Clients.api.getMyCourseProgress(runId)
                                            .enqueue(retrofitCallback { _, progressRes ->
                                                progressRes?.body().let {
                                                    var progress = 0.0
                                                    var completedContents = 0.0
                                                    var totalContents = 0.0
                                                    try {
                                                        progress = it?.get("percent") as Double
                                                        completedContents = it["completedContents"] as Double
                                                        totalContents = it["totalContents"] as Double
                                                    } catch (e: Exception) {
                                                        Crashlytics.logException(e)
                                                    }

                                                    val newCourse = courseRun.course?.run {
                                                        Course(
                                                            id,
                                                            title,
                                                            subtitle,
                                                            logo,
                                                            summary,
                                                            promoVideo,
                                                            difficulty,
                                                            reviewCount,
                                                            rating,
                                                            slug,
                                                            coverImage,
                                                            updatedAt,
                                                            categoryId
                                                        )
                                                    }
                                                    courseRun.run {
                                                        val newRun = CourseRun(
                                                            id,
                                                            runAttempts?.get(0)?.id ?: "",
                                                            name,
                                                            description,
                                                            enrollmentStart,
                                                            enrollmentEnd,
                                                            start,
                                                            end,
                                                            price,
                                                            mrp ?: "",
                                                            course?.id ?: "",
                                                            updatedAt,
                                                            progress,
                                                            course?.title ?: "",
                                                            summary = course?.summary ?: "",
                                                            premium = runAttempts?.get(0)?.premium
                                                                ?: false,
                                                            whatsappLink = whatsappLink ?: "",
                                                            crRunEnd = runAttempts?.get(0)?.end
                                                                ?: "",
                                                            totalContents = totalContents.toInt(),
                                                            completedContents = completedContents.toInt(),
                                                            mentorApproved = runAttempts?.get(0)?.certificateApproved
                                                                ?: false,
                                                            completionThreshold = completionThreshold,
                                                            productId = productId
                                                        )
                                                        val oldRun = runDao.getRunById(
                                                            runAttempts?.get(0)?.id ?: ""
                                                        )
                                                        if (oldRun == null) {
                                                            newCourse?.let { it1 ->
                                                                courseDao.insertNew(
                                                                    it1
                                                                )
                                                            }
                                                            runDao.insertNew(newRun)
                                                        } else if (oldRun.progress != progress || refresh) {
                                                            newRun.hits = oldRun.hits
                                                            newCourse?.let { it1 ->
                                                                courseDao.update(
                                                                    it1
                                                                )
                                                            }
                                                            runDao.update(newRun)
                                                        }
                                                    }

                                                    courseRun.course?.instructors?.forEach { instructorId ->
                                                        Clients.onlineV2JsonApi.instructorsById(
                                                            instructorId.id
                                                        ).enqueue(
                                                            retrofitCallback { _, instructorResponse ->
                                                                instructorResponse?.let {
                                                                    if (instructorResponse.isSuccessful) {
                                                                        instructorResponse.body()
                                                                            ?.run {
                                                                                instructorDao.insertNew(
                                                                                    Instructor(
                                                                                        id,
                                                                                        name,
                                                                                        description
                                                                                            ?: "",
                                                                                        photo,
                                                                                        updatedAt,
                                                                                        email,
                                                                                        sub
                                                                                    )
                                                                                )
                                                                                courseWithInstructorDao.insert(
                                                                                    CourseWithInstructor(
                                                                                        courseRun.course?.id
                                                                                            ?: "",
                                                                                        id
                                                                                    )
                                                                                )
                                                                            }
                                                                    }
                                                                }
                                                            })
                                                    }
                                                }
                                            })
                                    }
                                }
                            }
                            progress.value = false
                        }
                    }
                })
        } catch (e: Exception) {
            Log.i("Error", e.localizedMessage)
        }
    }

    fun fetchAllCourses() {
        Clients.onlineV2JsonApi.getAllCourses()
            .enqueue(retrofitCallback { _, response ->
                response?.let {
                    if (response.isSuccessful) {
                        it.body()?.let { courseList ->
                            courseList.forEach { course ->
                                course.run {
                                    courseDao.insertNew(
                                        Course(
                                            id,
                                            title,
                                            subtitle,
                                            logo,
                                            summary,
                                            promoVideo,
                                            difficulty,
                                            reviewCount,
                                            rating,
                                            slug,
                                            coverImage,
                                            updatedAt,
                                            categoryId,
                                            faq
                                        )
                                    )
                                }
                                var list = course.runs?.filter { runs ->
                                    !runs.enrollmentStart.greater() && runs.enrollmentEnd.greater() && !runs.unlisted
                                }?.sortedWith(compareBy { run -> run.price })
                                if (list != null) {
                                    if (list.isEmpty()) {
                                        list =
                                            course.runs?.sortedWith(compareBy { run -> run.price })
                                    }
                                }
                                list?.get(0)?.run {
                                    runDao.insertNew(
                                        CourseRun(
                                            id,
                                            "",
                                            name,
                                            description,
                                            enrollmentStart,
                                            enrollmentEnd,
                                            start,
                                            end,
                                            price,
                                            mrp ?: "",
                                            course.id,
                                            crUpdatedAt = updatedAt,
                                            title = course.title,
                                            summary = course.summary
                                        )
                                    )
                                }

                                course.instructors?.forEach { instructor ->
                                    instructorDao.insertNew(
                                        Instructor(
                                            instructor.id,
                                            instructor.name,
                                            instructor.description ?: "",
                                            instructor.photo,
                                            instructor.updatedAt,
                                            instructor.email,
                                            instructor.sub
                                        )
                                    )
                                    courseWithInstructorDao.insert(
                                        CourseWithInstructor(course.id, instructor.id)
                                    )
                                }
                            }
                        }
                    }
                    progress.value = false
                }
            })
    }

    fun fetchCards() {
        Clients.onlineV2JsonApi.carouselCards.enqueue(retrofitCallback { fallback, response ->
            response?.body()?.let {
                carouselCards.value = it
            }
        })
    }
}
