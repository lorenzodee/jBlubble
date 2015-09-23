<%@ include file="/WEB-INF/common/_taglibs.jspf" %>
<!DOCTYPE html>
<html>
<head><title>jBlubble Sample</title></head>
<body>
<h1>Edit Person</h1>
<form action="<c:url value='/persons/${person.id}' />" method="post" enctype="multipart/form-data">
	<input type="hidden" name="_method" value="PUT" />
	<div>
		<label>Name</label>
		<input type="text" name="name" />
	</div>
	<div>
		<label>Photo</label>
		<c:if test="${not empty person.photoId}"><div><img src="<c:url value='/persons/${person.id}/photo' />" /></div></c:if>
		<input type="file" name="photo" />
	</div>
	<div>
		<button>Update</button>
		<a href="<c:url value='/persons' />">Cancel</a>
	</div>
</form>
</body>
</html>
