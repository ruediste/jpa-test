package com.github.ruediste;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import test.Entity0;
import test.Entity17;

import com.github.ruediste.framework.entry.ApplicationInstance;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestApplicationInstance implements ApplicationInstance {

	private EntityManagerFactory emf;

	Logger log;

	@Inject
	TestSingleton testSingleton;

	public TestApplicationInstance() {
		Injector injector = Guice.createInjector(new InstanceModule());
		injector.injectMembers(this);

		log.info("Creation EMF");
		emf = Persistence.createEntityManagerFactory("my-app");
		log.info("initialization complete");
	}

	@Override
	public void handle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException,
			ServletException {

		log.info("Handling request to " + request.getRequestURI());

		long start = System.currentTimeMillis();
		int size = 0;
		try {
			EntityManagerFactory emf = Persistence
					.createEntityManagerFactory("my-app");
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			em.find(Entity0.class, 0);

			Entity0 en = new Entity0();
			Entity17 en2 = new Entity17();
			// en.setManyToOne83(en2);
			em.persist(en);
			em.persist(en2);

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Entity0> q = cb.createQuery(Entity0.class);
			Root<Entity0> root = q.from(Entity0.class);
			q.select(root);
			size = em.createQuery(q).getResultList().size();
			em.getTransaction().commit();
			emf.close();
		} catch (Throwable t) {
			log.error("Error in handle", t);
		}
		sendResponse(response, size);
		log.info("Handling time: " + (System.currentTimeMillis() - start));
	}

	protected void sendResponse(HttpServletResponse response, int size)
			throws IOException {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<h1> Hello Worlddd " + size + "</h1>");
		out.close();
	}

	@Override
	public void close() {
		emf.close();
	}

}
