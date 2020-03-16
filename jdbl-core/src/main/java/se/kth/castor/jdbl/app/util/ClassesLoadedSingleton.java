package se.kth.castor.jdbl.app.util;

import java.util.Set;

public enum ClassesLoadedSingleton
{
   INSTANCE;

   private Set<String> classesLoaded;

   public void setClassesLoaded(Set<String> classesLoaded)
   {
      this.classesLoaded = classesLoaded;
   }

   public Set<String> getClassesLoaded()
   {
      return this.classesLoaded;
   }

   public void printClassesLoaded()
   {
      this.classesLoaded.forEach(s -> System.out.println("Loaded: " + s));
   }

}
