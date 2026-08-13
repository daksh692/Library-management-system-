
use lms

db.books.createIndex(
  { name: "text", author: "text", genre: "text", shortDescription: "text" },
  { name: "book_text_search", weights: { name: 10, author: 5, genre: 3, shortDescription: 1 } }
)

db.books.createIndex({ isbn: 1 })
db.books.createIndex({ isDeleted: 1, createdAt: -1 })      // new arrivals
db.books.createIndex({ isDeleted: 1, genre: 1 })            // recommendations

db.users.createIndex({ phone: 1 }, { unique: true })
db.users.createIndex({ userId: 1 }, { unique: true })
db.users.createIndex({ isDeleted: 1 })

db.notifications.createIndex({ userId: 1, read: 1, createdAt: -1 })

print("Indexes created.");
printjson(db.transactions.getIndexes());

