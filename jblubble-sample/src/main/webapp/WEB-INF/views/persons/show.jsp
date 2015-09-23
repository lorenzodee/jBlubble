<%@ include file="/WEB-INF/common/_taglibs.jspf" %><!DOCTYPE html>
<html>
<head><title><c:out value="${person.name}" /> - jBlubble Sample</title></head>
<body>
<h1><c:out value="${person.name}" /></h1>
<c:if test="${not empty person.photoId}">
<p><img src="<c:url value='/persons/${person.id}/photo' />" /></p>
</c:if>
</body>
</html>
