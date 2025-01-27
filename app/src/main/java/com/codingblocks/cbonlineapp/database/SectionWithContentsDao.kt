package com.codingblocks.cbonlineapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codingblocks.cbonlineapp.database.models.CourseContent
import com.codingblocks.cbonlineapp.database.models.SectionWithContent

@Dao
interface SectionWithContentsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(join: SectionWithContent)
    @Query("""
        SELECT * FROM CourseContent
        INNER JOIN sectionwithcontent ON
        CourseContent.id = sectionwithcontent.content_id
        WHERE sectionwithcontent.section_id = :sectionID ORDER BY `order`
        """)
    fun getContentWithSectionId(sectionID: String): LiveData<List<CourseContent>>

    @Query("""
        SELECT * FROM CourseContent cc
        INNER JOIN sectionwithcontent swc ON
        cc.id = swc.content_id
        WHERE swc.section_id = :sectionID AND cc.contentable = "lecture"
        AND isDownloaded = "false" ORDER BY `order`
        """)
    fun getVideoIdsWithSectionId(sectionID: String): LiveData<List<CourseContent>>

    @Query("""
        SELECT * FROM  CourseSection s
	    INNER JOIN SectionWithContent sc ON sc."section_id" = s."uid"
	    INNER JOIN CourseContent c ON c."uid" = sc."content_id"
	    WHERE s.attempt_id = :attemptId AND progress = "UNDONE"
        ORDER BY s."sectionOrder", sc."order" LIMIT 1;
        """)
    fun resumeCourse(attemptId: String): LiveData<List<CourseContent>>

//    @Query("""
//        SELECT * FROM  CourseSection s
// 	    INNER JOIN SectionWithContent sc ON sc."section_id" = s."uid"
// 	    INNER JOIN CourseContent c ON c."uid" = sc."content_id"
// 	    WHERE s.attempt_id = :attemptId AND contentable = "lecture" OR contentable = "video" AND
//        Select order FROM c WHERE id = :uid AND
//        LIMIT 1;
//        """)
//    fun getNextVideo(attemptId: String): LiveData<List<CourseContent>>
}
