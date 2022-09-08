package com.Natrec.DocHandler.Repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Natrec.DocHandler.Model.ScanDoc;

@Repository
public interface ScanDocRepository extends JpaRepository<ScanDoc, Long>{
	@Query(nativeQuery = true, value = "SELECT SM_LOB_PKG.GET_DOC_FILE_NAME(:text) FROM dual")
	String getFileName(@Param("text") String text);

}
