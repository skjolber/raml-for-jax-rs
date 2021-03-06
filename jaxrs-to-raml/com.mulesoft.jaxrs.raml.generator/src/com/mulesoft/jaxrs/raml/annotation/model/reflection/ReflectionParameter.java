package com.mulesoft.jaxrs.raml.annotation.model.reflection;

import java.lang.annotation.Annotation;

import com.mulesoft.jaxrs.raml.annotation.model.IAnnotationModel;
import com.mulesoft.jaxrs.raml.annotation.model.IParameterModel;

public class ReflectionParameter implements IParameterModel{

	private static final String VALUE = "value";
	protected ReflectionType type;
	public ReflectionParameter(ReflectionType type, AnnotationModel[] model) {
		super();
		this.type = type;
		this.model = model;
	}
	
	public ReflectionParameter(Class<?> cl, Annotation[] annotations) {
		this.type=new ReflectionType(cl);
		model=new AnnotationModel[annotations.length];
		int i=0;
		for (Annotation a:annotations){
			model[i++]=new AnnotationModel(a);
		}
	}
	
	protected AnnotationModel[] model;

	
	public String getDocumentation() {
		return "";
	}
	
	
	public String getAnnotationValue(String annotation) {
		IAnnotationModel[] annotations = getAnnotations();
		for (IAnnotationModel q:annotations){
			if (q.getName().equals(annotation)){
				return q.getValue(VALUE);
			}
		}
		return null;
	}

	
	public String[] getAnnotationValues(String annotation) {
		IAnnotationModel[] annotations = getAnnotations();
		for (IAnnotationModel q:annotations){
			if (q.getName().equals(annotation)){
				return q.getValues(annotation);
			}
		}
		return null;
	}

	
	public boolean hasAnnotation(String name) {
		IAnnotationModel[] annotations = getAnnotations();
		for (IAnnotationModel q:annotations){
			if (q.getName().equals(name)){
				return true;
			}
		}
		return false;
	}
	
	public IAnnotationModel getAnnotation(String name) {
		IAnnotationModel[] annotations = getAnnotations();
		for (IAnnotationModel m:annotations){
			if (m.getName().equals(name)){
				return m;
			}
		}
		return null;
	}
	
	public String getName() {
		String name = type.getName();
		return name;
	}
	
	public String getType() {
		String name = type.getName();		
		return name;
	}
	
	public boolean required() {
		return type.element.isPrimitive();
	}
	
	public IAnnotationModel[] getAnnotations() {
		return model;
	}
}
