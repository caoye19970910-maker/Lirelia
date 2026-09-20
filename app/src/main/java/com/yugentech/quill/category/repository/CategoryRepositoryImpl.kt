package com.yugentech.quill.category.repository

import com.yugentech.quill.database.dao.CategoryDao
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.theme.tokens.AppConstants.SHELF
import kotlinx.coroutines.flow.Flow

/**
 * Local-only category storage for Lirelia.
 *
 * Cloud synchronization from the original Quill project is intentionally
 * removed from the clean baseline. Categories are persisted with Room.
 */
class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override fun getUserCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getUserCategories()

    override suspend fun getCategoryCount(): Int =
        categoryDao.getCategoryCount()

    override suspend fun initializeDefaultCategories() {
        val systemShelf = CategoryEntity(
            name = SHELF,
            sortOrder = 99,
            isSystem = true,
            isSynced = true
        )
        categoryDao.insertCategory(systemShelf)
    }

    override suspend fun insertCategory(name: String) {
        categoryDao.insertCategory(
            CategoryEntity(
                name = name,
                sortOrder = categoryDao.getCategoryCount(),
                isSystem = false,
                isSynced = true
            )
        )
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category.copy(isSynced = true))
    }

    override suspend fun updateCategories(categories: List<CategoryEntity>) {
        categoryDao.updateCategories(categories.map { it.copy(isSynced = true) })
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category.name)
    }
}
