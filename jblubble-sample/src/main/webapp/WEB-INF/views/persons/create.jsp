<%@ include file="/WEB-INF/common/_taglibs.jspf" %>
<!DOCTYPE html>
<html>
<head><title>jBlubble Sample</title></head>
<body>
<h1>Create Person</h1>
<form action="<c:url value='/persons' />" method="post" enctype="multipart/form-data">
	<div>
		<label>Name</label>
		<input type="text" name="name" />
	</div>
	<div>
		<label>Photo</label>
		<input type="file" name="photo" />
	</div>
	<div>
		<button>Create</button>
		<a href="<c:url value='/persons' />">Cancel</a>
	</div>
</form>
</body>
</html>
