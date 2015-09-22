<%@ include file="/WEB-INF/common/_taglibs.jspf" %><!DOCTYPE html>
<html>
<head><title>jBlubble Sample</title></head>
<body>
<h1>Uploads</h1>
<p>Upload a test file <a href="<c:url value='/uploads/create' />">here</a>.</p>
<c:if test="${not empty blobInfos}">
<table border="1" cellpadding="10">
	<tr>
		<th>Key</th><th>Name</th><th>Content Type</th><th>Length</th>
	</tr><c:forEach var="blobInfo" items="${blobInfos}">
	<tr>
		<td><a href="<c:url value='/uploads/${blobInfo.blobKey}' />"><c:out value="${blobInfo.blobKey}" /></a></td>
		<td><a href="<c:url value='/uploads/${blobInfo.blobKey}' />"><c:out value="${blobInfo.name}" /></a></td>
		<td><c:out value="${blobInfo.contentType}" /></td>
		<td align="right"><fmt:formatNumber value="${blobInfo.size}" /></td>
	</tr>
	</c:forEach>
</table>
</c:if>
</body>
</html>
