package backend;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

//this class is started when the server starts. 

public class ContextListener implements ServletContextListener{
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
				
		//manually load JDBC driver (needed for Tomcat)
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
		}
		
		//create instance of SQL (admin) CRUD class
		try {
			SQLCRUD sqlCrud = new SQLCRUD("AdminDB", "jdbc:mysql://localhost:3306", "root", "root");
			
			assert(sqlCrud != null);
			//store the crud instance in the servlet context so that it could be accessed by other components
			context.setAttribute("sqlCrud", sqlCrud);
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		//used for cleanup
	}
}
