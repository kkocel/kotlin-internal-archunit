package tech.kocel.kotlin.archunit.internal

import com.tngtech.archunit.PublicAPI
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent

object KotlinInternalDependencyRules {
    @PublicAPI(usage = PublicAPI.Usage.ACCESS)
    fun accessClassesThatResideInAnOuterPackage(): ArchCondition<JavaClass> {
        return AccessClassesThatResideInAnOuterPackageCondition()
    }

    @PublicAPI(usage = PublicAPI.Usage.ACCESS)
    fun accessClassesThatResideInASubpackage(): ArchCondition<JavaClass> {
        return AccessClassesThatResideInASubpackageCondition()
    }

    private fun getOutermostEnclosingClass(javaClass: JavaClass): JavaClass {
        var clazz = javaClass
        while (clazz.enclosingClass.isPresent) {
            clazz = clazz.enclosingClass.get()
        }
        return clazz
    }

    private class AccessClassesThatResideInAnOuterPackageCondition :
        ArchCondition<JavaClass>("access classes that reside in an upper package") {
        override fun check(clazz: JavaClass, events: ConditionEvents) {
            for (access in clazz.accessesFromSelf) {
                val callToSuperPackage = isCallToSuperPackage(access.originOwner, access.targetOwner)
                events.add(SimpleConditionEvent(access, callToSuperPackage, access.description))
            }
        }

        private fun isCallToSuperPackage(origin: JavaClass, target: JavaClass): Boolean {
            val originPackageName = getOutermostEnclosingClass(origin).packageName
            val targetSubPackagePrefix = getOutermostEnclosingClass(target).packageName + "."
            return originPackageName.startsWith(targetSubPackagePrefix)
        }
    }

    private class AccessClassesThatResideInASubpackageCondition :
        ArchCondition<JavaClass>("access classes that reside in a lower package") {
        override fun check(clazz: JavaClass, events: ConditionEvents) {
            for (access in clazz.accessesFromSelf) {
                val callToLowerPackage = isCallToLowerPackage(access.originOwner, access.targetOwner)
                events.add(SimpleConditionEvent(access, callToLowerPackage, access.description))
            }
        }

        private fun isCallToLowerPackage(origin: JavaClass, target: JavaClass): Boolean {
            if (!target.reflect().isInternal()) {
                return false
            }
            val originPackageName = getOutermostEnclosingClass(origin).packageName
            val targetSubPackagePrefix = getOutermostEnclosingClass(target).packageName
            return targetSubPackagePrefix.startsWith(originPackageName) && targetSubPackagePrefix != originPackageName
        }
    }
}